package com.smartfinance.statement;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum FileType {
	 CSV("csv"), XLSX("xlsx"), XLS("xls");

	    private final String extension;

	    FileType(String extension) {
	        this.extension = extension;
	    }

	    public static Optional<FileType> fromFileName(String fileName) {
	        if (fileName == null) {
	            return Optional.empty();
	        }
	        int dot = fileName.lastIndexOf('.');
	        if (dot < 0 || dot == fileName.length() - 1) {
	            return Optional.empty();
	        }
	        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
	        return Arrays.stream(values()).filter(type -> type.extension.equals(ext)).findFirst();
	    }
}
