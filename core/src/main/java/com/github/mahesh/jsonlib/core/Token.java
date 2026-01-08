package com.github.mahesh.jsonlib.core;

public final class Token {
    private final TokenType tokenType;
    private final String value;
    private final int position;

    public Token(TokenType tokenType, String value, int position) {
        this.tokenType = tokenType;
        this.value = value;
        this.position = position;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public String getValue() {
        return value;
    }

    public int getPosition() {
        return position;
    }
}
