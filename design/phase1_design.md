# Phase 1 — JSON Library Core Design (Tokenizer, Parser, Serializer)

This document describes the **conceptual design** for the JSON library’s core components.  
It defines classes, fields, enums, configs, exceptions, and how they interact — without implementation code.

---

## 🎯 Goal (Input → Output)

- **Input**: JSON string or character stream (String, Reader, or InputStream).  
- **Process**:  
  1. **Tokenizer** → characters → tokens  
  2. **Parser** → tokens → `JsonValue` AST tree  
  3. **Serializer** → AST → JSON text (compact or pretty)  
- **Output**:  
  - Success → `JsonValue` tree and/or JSON string  
  - Failure → structured exception with path + location  

---

## 1. Core Classes

### 1.1 `Token` (immutable)
- Fields:
  - `tokenType: TokenType`
  - `startIndex: int`
  - `endIndex: int`
  - `raw: String` (raw lexeme, e.g., `"123"`)
  - `value: Object` (optional parsed primitive)
  - `line: int`
  - `column: int`
- Purpose: smallest unit emitted by Tokenizer.

---

### 1.2 `TokenType` (enum)
- Members:  
  `LEFT_BRACE`, `RIGHT_BRACE`,  
  `LEFT_BRACKET`, `RIGHT_BRACKET`,  
  `COLON`, `COMMA`,  
  `STRING`, `NUMBER`,  
  `TRUE`, `FALSE`, `NULL`,  
  `EOF`.

---

### 1.3 `Tokenizer`
- Fields:
  - `inputSource: Reader/char buffer`
  - `config: TokenizerConfig`
  - `currentIndex, line, column`
- Methods:
  - `hasNextToken()`
  - `nextToken()`
  - `peekToken()`
- Responsibilities:
  - Skip whitespace  
  - Recognize structural tokens (`{`, `}`, `[`, `]`, `,`, `:`)  
  - Handle strings (escapes + unicode)  
  - Handle numbers (int, float, exponents)  
  - Recognize keywords (`true`, `false`, `null`)  
  - Enforce limits from config  

---

### 1.4 `JsonValue` (abstract/interface)
- Methods:
  - `getType(): JsonType`
  - `asStringOptional()`
  - `asNumberOptional()`
  - `asBooleanOptional()`
  - `asObjectOptional()`
  - `asArrayOptional()`
- Notes:
  - Immutable nodes
  - Deep structural equality

---

### 1.5 `JsonType` (enum)
- Members: `OBJECT`, `ARRAY`, `STRING`, `NUMBER`, `BOOLEAN`, `NULL`.

---

### 1.6 `JsonObject`
- Fields:
  - `entries: Map<String, JsonValue>` (insertion-ordered)
- API:
  - `get(String key)`
  - `getString(String key)`
  - `keySet()`, `entrySet()`, `containsKey()`
- Built via `JsonObjectBuilder`.

---

### 1.7 `JsonArray`
- Fields:
  - `values: List<JsonValue>`
- API:
  - `get(int index)`
  - `asList()`
- Built via `JsonArrayBuilder`.

---

### 1.8 `JsonString`, `JsonNumber`, `JsonBoolean`, `JsonNull`
- `JsonString`: holds a string value (escaped/unescaped).  
- `JsonNumber`: holds numeric lexeme string + lazy parsed forms (`BigDecimal`, `long`, `double`).  
- `JsonBoolean`: holds `true`/`false`.  
- `JsonNull`: singleton representing `null`.  

---

### 1.9 Builders
- `JsonObjectBuilder` → builds immutable `JsonObject`.  
- `JsonArrayBuilder` → builds immutable `JsonArray`.  

---

### 1.10 `JsonParser`
- Fields:
  - `tokenizer: Tokenizer`
  - `config: ParserConfig`
  - `pathStack: Deque<PathElement>`
  - `depthCounter: int`
- Methods:
  - `parse(String/Reader)`
  - `parseObject()`
  - `parseArray()`
  - `parsePrimitive()`
- Responsibilities:
  - Construct AST nodes
  - Track JSON path for errors
  - Enforce depth limits & duplicate key policies

---

### 1.11 `Serializer`
- Fields:
  - `config: SerializerConfig`
  - `writer: Appendable`
- Methods:
  - `serialize(JsonValue)`
  - `serializePretty(JsonValue)`
- Responsibilities:
  - Traverse AST depth-first
  - Handle string escaping
  - Format compact/pretty output

---

## 2. Config Objects & Enums

### 2.1 `TokenizerConfig`
- Fields:
  - `lenient: boolean`
  - `allowComments: boolean`
  - `maxTokenLength: int`
  - `maxInputSize: long`

### 2.2 `ParserConfig`
- Fields:
  - `lenient: boolean`
  - `maxNestingDepth: int`
  - `duplicateKeyPolicy: DuplicateKeyPolicy`

### 2.3 `SerializerConfig`
- Fields:
  - `prettyPrint: boolean`
  - `indent: String`
  - `trailingComma: boolean`
  - `maxLineLength: int`

### 2.4 `DuplicateKeyPolicy` (enum)
- Members: `ERROR`, `KEEP_LAST`, `KEEP_FIRST`.

---

## 3. Exceptions

### 3.1 `JsonLexException`
- From Tokenizer
- Fields:
  - `message`
  - `startIndex`, `endIndex`
  - `line`, `column`
  - `snippet`

### 3.2 `JsonParseException`
- From Parser
- Fields:
  - `message`
  - `jsonPath` (e.g. `$.users[1].name`)
  - `token`
  - `index`, `line`, `column`
  - `snippet`

### 3.3 `JsonSerializeException`
- From Serializer
- Fields:
  - `message`
  - `cause`
  - `node`

---

## 4. Future Annotations (Phase 3+)
- `@JsonProperty`
- `@JsonIgnore`
- `@JsonCreator`
- `@JsonSerialize(using=...)`
- `@JsonDeserialize(using=...)`

*(Not implemented in Phase 1, but design should anticipate them.)*

---

## 5. Runtime Sequence (Walkthrough)

Input:  
```json
{ "users": [ { "id": 1, "name": "A" }, { "id": 2, "name": "B" } ] }
```

1. **Tokenizer** emits tokens:
   - `{`, `"users"`, `:`, `[`, `{`, `"id"`, `:`, `1`, `,`, `"name"`, `:`, `"A"`, `}`, `,`, `{`, ... `]`, `}`, EOF

2. **Parser** consumes tokens:
   - Builds `JsonObject` with key `users`
   - `users` → `JsonArray`
   - Array contains 2 `JsonObject`s (`id=1, name=A`; `id=2, name=B`)

3. **Serializer** (compact):
   ```json
   {"users":[{"id":1,"name":"A"},{"id":2,"name":"B"}]}
   ```

4. **Error case** (missing closing brace):
   - Throw `JsonParseException` with:
     - message: `"Expected RIGHT_BRACE but found EOF"`
     - jsonPath: `"$.users[1]"`
     - index/line/column
     - snippet

---

## 6. Testing Strategy

### Tokenizer tests
- Structural tokens `{ } [ ] , :`
- Strings with escapes (`
`, `ሴ`)
- Numbers: ints, negatives, decimals, exponents
- Keywords: `true`, `false`, `null`
- EOF handling
- Config limits enforcement

### Parser tests
- Simple and nested objects/arrays
- Malformed JSON errors with paths
- Duplicate key policy
- Depth limit

### Serializer tests
- Compact vs pretty
- Roundtrip parse → serialize → parse
- Escaping rules

### Integration tests
- Random JSON generator roundtrip
- Fuzz testing for robustness

---

## 7. Performance, Safety, Extensibility

- **Performance**: minimize string copies, lazy parse numbers, builders during parse.  
- **Safety**: not thread-safe (per-instance use). Document this.  
- **Extensibility**: keep Tokenizer/Parser stable, support adapters/modules in later phases.  

---
