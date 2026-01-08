package com.github.mahesh.jsonlib.core;



/**
 * Represents a lexical token produced by the JSON tokenizer.
 * <p>
 * A {@code Token} is an immutable data holder containing the token type,
 * its textual value (lexeme), and the position in the input where the token
 * begins.
 * </p>
 *
 * <p>
 * Tokens are intentionally simple and do not perform any parsing or validation.
 * They are consumed by higher-level components such as parsers or streaming readers.
 * </p>
 */
public final class Token {
    private final TokenType tokenType;
    private final String value;
    private final int position;

    /**
     * Creates a new {@code Token}.
     *
     * @param tokenType the type of the token (must not be {@code null})
     * @param value the textual value of the token (may be {@code null} for EOF)
     * @param position the zero-based character position in the input
     * @throws NullPointerException if {@code tokenType} is {@code null}
     */
    public Token(TokenType tokenType, String value, int position) {
        if (tokenType == null) {
            throw new NullPointerException("tokenType must not be null");
        }
        this.tokenType = tokenType;
        this.value = value;
        this.position = position;
    }

    /**
     * Returns the type of this token.
     *
     * @return the token type
     */
    public TokenType getTokenType() {
        return tokenType;
    }

    /**
     * Returns the textual value of this token.
     *
     * @return the token value, or {@code null} for tokens without a value (e.g., EOF)
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the position in the input where this token starts.
     *
     * @return zero-based character position
     */
    public int getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return tokenType + "(" + value + ")@" + position;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Token)) return false;
        Token other = (Token) obj;
        return tokenType == other.tokenType &&
                (value == null ? other.value == null : value.equals(other.value));
    }

    @Override
    public int hashCode() {
        int result = tokenType.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
