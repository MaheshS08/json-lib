package com.github.mahesh.jsonlib.core.lexer;
/**
 * A JSON tokenizer (lexer) that converts a JSON input string into
 * a sequence of lexical {@link Token}s.
 * This class is final to ensure consistent and predictable
 * lexical analysis behavior across the library.
 * Extension should be achieved via composition rather than inheritance.f
 *
 * <p>
 * The tokenizer reads the input from left to right and produces tokens
 * such as structural symbols, strings, numbers, boolean literals, and null.
 * </p>
 *
 * <p>
 * This component does not validate JSON structure and does not build
 * object trees. It is intended to be used by a parser or a streaming reader.
 * </p>
 *
 * <p>
 * The tokenizer is stateful and is not thread-safe.
 * </p>
 */
public class JsonTokenizer {

    /** The complete JSON input. */
    private final String input;

    /** Length of the input for bounds checking. */
    private final int length;

    /** Current cursor position in the input. */
    private int position;

    /**
     * Creates a new tokenizer for the given JSON input.
     *
     * @param input the JSON input string
     * @throws NullPointerException if input is null
     */
    public JsonTokenizer(String input) {
        if (input == null) {
            throw new NullPointerException("input must not be null");
        }
        this.input = input;
        this.length = input.length();
        this.position = 0;
    }

    /**
     * Returns the next lexical token from the input.
     *
     * <p>
     * Each invocation consumes characters from the input and produces
     * exactly one token. Whitespace is skipped automatically.
     * </p>
     *
     * <p>
     * When the end of input is reached, an EOF token is returned.
     * </p>
     */
    public Token nextToken() {
        // Implementation will be added step by step
        return new Token(TokenType.EOF, null, position);
    }
}
