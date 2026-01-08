# core/DESIGN.md

# Core module — DESIGN (Tokenizer / Parser / Serializer)

## Purpose
The `core` module provides the foundational JSON functionality:
- Lexical analysis (Tokenizer): converts input characters into tokens.
- Parsing (Parser): converts tokens into an in-memory AST (`JsonValue` tree).
- Serialization (Serializer): converts AST back to JSON text.
  This module is designed to be correct, well-tested, and a stable base for higher-level features.

---

## High-level approach & rationale
- **Parser style:** Recursive-descent parser driven by a Tokenizer. Chosen for clarity, ease of testing, and learning value. Performance optimization (e.g., hand-rolled state machine) can be done later guided by JMH.
- **Tokenizer vs Parser responsibilities:** Tokenizer handles char-level concerns (escapes, unicode, numbers formats) and emits atomic tokens. Parser consumes tokens and builds AST, handling structural semantics (objects, arrays, commas, colons).
- **AST model:** Immutable public-facing node types with builders for construction (immutability helps reasoning; builders help incremental construction during parsing).
- **Error reporting:** Errors include JSONPath-like context, character index, line/column when available, and token-level diagnostics.

---

## Public API sketches (docs only)
(Only method signatures / behavior—no implementation code here.)

### Tokenizer (concept)
- Construction: `Tokenizer.from(String|Reader|InputStream, TokenizerConfig config)`
- Iteration:
    - `boolean hasNextToken()`
    - `Token nextToken()`
    - `Token peekToken()` (non-consuming)
- Reset: either `reset()` or create new instance for new input
- Error behavior: throws `JsonLexException` on unrecoverable lexical errors; in lenient mode may skip comments/trailing commas.

### Parser (concept)
- Construction: `JsonParser(ParserConfig config)` or static parse helpers.
- Parse methods:
    - `JsonValue parse(String json)`
    - `JsonValue parse(Reader reader)`
    - `Optional<JsonValue> tryParse(...)` (lenient failure returns empty or result wrapper)
- Error behavior: throws `JsonParseException` with path and index.

### JsonValue (AST)
- Node subtypes: `JsonObject`, `JsonArray`, `JsonString`, `JsonNumber`, `JsonBoolean`, `JsonNull`.
- Read helpers: `Optional<T> as(Class<T>)` or typed getters `getString(key)` etc (provide safe vs strict variants).
- Builders: `JsonObject.builder()` / `JsonArray.builder()` for constructing during parsing.

### Serializer (concept)
- Construction: `Serializer.builder().config(SerializerConfig).build()`
- Methods:
    - `String serialize(JsonValue value)`
    - `void write(JsonValue value, Writer)`
- Modes: compact and pretty with indent options.

---

## Data structures and invariants

### Token
- Immutable structure:
    - `TokenType` (enum): `{, }, [, ], :, ,, STRING, NUMBER, TRUE, FALSE, NULL, EOF`
    - `int startIndex, int endIndex` (char offsets)
    - `String raw` (raw lexeme when useful)
    - `Object value` (optional parsed value e.g., numeric as String to avoid early loss of precision)
- Invariant: `startIndex <= endIndex`, token's `raw` length equals `endIndex - startIndex`.

### JsonValue / Subtypes
- Public APIs expose safe accessors that either return `Optional` or throw `JsonMappingException` for conversions.
- Internally prefer immutable containers (unmodifiable maps/lists), constructed via builders to reduce GC churn during parsing.

---

## Configurable options (Config objects)
- `TokenizerConfig`
    - `boolean lenient` (allow comments, trailing commas)
    - `int maxTokenLength`
    - `int maxInputSize`
- `ParserConfig`
    - `boolean lenient` (mirror Tokenizer)
    - `int maxNestingDepth`
    - `boolean allowDuplicateKeys` (configurable: last-wins or error)
- `SerializerConfig`
    - `boolean pretty`
    - `String indent` (e.g., "  ")
    - `boolean trailingComma`
    - `int maxLineLength` (for optional wrapping heuristics)

Config objects are immutable and built via builder pattern.

---

## Error handling
- Domain exceptions:
    - `JsonLexException` (tokenizer-level): includes index, message, snippet.
    - `JsonParseException` (parser-level): includes JSON path (e.g., `$.users[3].name`), token expected/actual, index, line/column.
    - `JsonSerializeException` (serializer-level): message and context.
- Exception messages MUST include actionable data: path, index, and a short snippet of offending input when feasible.

---

## Tokenizer behavior & edge cases (detailed)
- **Strings**
    - Must support all JSON escapes: `\" \\ / b f n r t` and `\uXXXX` hex escapes.
    - Properly handle surrogate pairs produced by `\u` sequences; produce correct UTF-16/UTF-8 output.
    - Reject unescaped control characters in strict mode.
- **Numbers**
    - Accept forms: `-?INT(.DIGITS)?([eE][+-]?DIGITS)?`
    - Keep numeric lexeme as a `String` in Token to avoid lossy conversion; conversion to number types handled later or by mapper.
    - Reject leading zeros unless zero is the whole integer (strict mode).
- **Whitespace**
    - Recognize Unicode whitespace per JSON spec; optionally ignore comments in lenient mode (C/C++ style or `#`? document choices).
- **Leniency**
    - Define precisely what lenient allows: trailing commas, comments, single-quoted strings, etc. Default should be strict. Document options.

---

## Parser design specifics
- **Entry point** will call `parseValue()` which dispatches by peeking token type.
- **Object parsing**
    - Expect `{`, then loop: if `}`, return empty object; else read `STRING` token for key (or lenient allow unquoted?), expect `:`, parse value, handle `,` vs `}`.
    - If `allowDuplicateKeys=false` and duplicate encountered, throw parse exception or provide configurable policy.
- **Array parsing**
    - Expect `[`, then loop: if `]`, return empty array; else parse value, handle `,` vs `]`.
- **State management**
    - Maintain `Stack<Context>` for building JSON path during parse (useful for error messages).
    - Track depth and enforce `maxNestingDepth`.
- **Recovery**
    - No complex recovery required for Phase 1—fail fast with good diagnostics.

---

## Serializer design specifics
- **Traversal**
    - Depth-first traversal of AST and write out characters into `Writer` (avoid building large intermediate strings).
- **Pretty printing**
    - Maintain indentation depth and write newlines/indent based on config.
- **Escaping**
    - Use deterministic escaping logic for control chars and quotes. Keep same escape rules as tokenizer to ensure roundtrip.
- **Stable ordering**
    - For `JsonObject` maintain insertion order (LinkedHashMap behavior) so roundtrips are predictable.

---

## Testing strategy (core-focused & required)
- Unit tests for tokenizer, parser, serializer with deterministic inputs and expected outputs/errors.
- Property tests for roundtrip (random JSON -> parse -> serialize -> parse -> structural equality).
- Edge-case tests: deeply nested structures, huge numbers, various unicode edge cases.
- Security tests: ensure config limits (max nesting / token length) are enforced.
- Performance micro-tests: sanity checks on large inputs (not full JMH here, just ensure no OOM on moderate large test inputs).

---

## Observability & metrics (optional hooks)
- Provide optional counters/hooks for:
    - tokens produced, bytes parsed, parse time (per parse), max depth encountered.
- Expose via callback interface or lightweight listener for benchmarking.

---

## Non-functional requirements
- **Correctness first**: pass JSON spec edge cases used in tests.
- **Readability & maintainability**: clear, small functions; prefer correctness over micro-optimizations initially.
- **Test coverage**: aim for high coverage on tokenizer and parser.
- **Security**: protect against DoS via configurable limits.

---

## Decisions to record (examples)
- Chosen parser style: recursive descent (record reasoning).
- Token representation: keep numeric lexeme as `String` in Token to avoid early numeric conversion.
- AST mutability: public API immutable, internal builders mutable during parse.
- Default modes: strict by default; lenient opt-in.

---

## Appendices
- **Error message examples**:
    - `JsonParseException: Unexpected token ']' at $.users[2].name (char 153, line 7, col 3): ...`
- **Common pitfalls**:
    - Not handling `\u` surrogate pairs properly.
    - Using `String.substring` excessively causing GC pressure—prefer char[] parsing where necessary during optimizations.
- **Next steps after Phase 1**:
    - Add `ObjectMapper` that maps AST -> POJOs.
    - Implement streaming parser that bypasses AST for large inputs.

