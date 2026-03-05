package com.github.mahesh.jsonlib;

import com.github.mahesh.jsonlib.core.lexer.JsonTokenizer;
import com.github.mahesh.jsonlib.core.lexer.Token;
import com.github.mahesh.jsonlib.core.lexer.TokenType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LexerTest {
    JsonTokenizer jsonTokenizer;

    @Test
    public void shouldTokenizeEmptyObject() {
        jsonTokenizer = new JsonTokenizer("{:}");
//        Token t1 = jsonTokenizer.nextToken();
//        Token t2 = jsonTokenizer.nextToken();
//        Token t3 = jsonTokenizer.nextToken();
//
//        assertEquals(TokenType.LEFT_BRACE, t1.getTokenType());
//        assertEquals(TokenType.RIGHT_BRACE, t2.getTokenType());
//        assertEquals(TokenType.EOF, t3.getTokenType());

        List<TokenType> tokens = new ArrayList<>();

        Token token;
        do {
            token = jsonTokenizer.nextToken();
            tokens.add(token.getTokenType());
        } while (token.getTokenType() != TokenType.EOF);

        assertEquals(
                List.of(TokenType.LEFT_BRACE, TokenType.COLON, TokenType.RIGHT_BRACE, TokenType.EOF),
                tokens
        );

        tokens.forEach(System.out::println);

    }
}
