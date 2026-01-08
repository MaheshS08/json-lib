package com.github.mahesh.jsonlib.core;

import java.io.Closeable;

/**
 * Types of tokens emitted by the Tokenizer during lexing.
 */
public enum TokenType {
    // Structural tokens
    LEFT_BRACE,     // {
    RIGHT_BRACE,    // }
    LEFT_BRACKET,   // [
    RIGHT_BRACKET,  // ]
    COMMA,          // ,
    COLON,          // :

    // Literals
    STRING,         // "abc"
    NUMBER,         // 123, -4.5, 1e10
    TRUE,           // true
    FALSE,          // false
    NULL,           // null

    // End of input
    EOF             // end-of-file marker


}