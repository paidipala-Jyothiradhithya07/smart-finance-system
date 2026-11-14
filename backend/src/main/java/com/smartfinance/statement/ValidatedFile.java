package com.smartfinance.statement;

public record ValidatedFile(String fileName, FileType type, byte[] content) {

}
