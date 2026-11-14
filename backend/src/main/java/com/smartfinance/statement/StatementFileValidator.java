package com.smartfinance.statement;

import java.io.IOException;
import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.smartfinance.config.StatementProperties;
import com.smartfinance.exception.FileProcessingException;
import com.smartfinance.exception.InvalidFileException;

@Component
public class StatementFileValidator {
	 private static final int SNIFF_BYTES = 4096;

	    private final StatementProperties properties;

	    public StatementFileValidator(StatementProperties properties) {
	        this.properties = properties;
	    }

	    public ValidatedFile validate(MultipartFile file) {
	        if (file == null || file.isEmpty()) {
	            throw new InvalidFileException("The file is empty");
	        }
	        String fileName = safeFileName(file.getOriginalFilename());
	        FileType type = FileType.fromFileName(fileName)
	                .orElseThrow(() -> new InvalidFileException(unsupportedMessage(fileName)));

	        if (file.getSize() > properties.maxFileSizeBytes()) {
	            throw new InvalidFileException("The file is larger than " + properties.getMaxFileSizeMb() + " MB");
	        }

	        byte[] content;
	        try {
	            content = file.getBytes();
	        } catch (IOException ex) {
	            throw new FileProcessingException("The file could not be read");
	        }

	        checkContent(type, content);
	        return new ValidatedFile(fileName, type, content);
	    }

	    /** Strips any folder part and control characters, so the name is safe to show and store. */
	    public static String safeFileName(String original) {
	        String name = original == null ? "" : original.replace('\\', '/');
	        name = name.substring(name.lastIndexOf('/') + 1);
	        name = name.replaceAll("\\p{Cntrl}", "").trim();
	        if (name.length() > 150) {
	            name = name.substring(name.length() - 150);
	        }
	        return name.isEmpty() ? "statement" : name;
	    }

	    private static String unsupportedMessage(String fileName) {
	        if (fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
	            return "PDF statements are not supported yet. Please upload a CSV or Excel (.xlsx, .xls) file.";
	        }
	        return "Unsupported file type. Please upload a CSV or Excel (.xlsx, .xls) file.";
	    }

	    private static void checkContent(FileType type, byte[] content) {
	        boolean zip = startsWith(content, 0x50, 0x4B, 0x03, 0x04);          // "PK" (xlsx is a zip file)
	        boolean ole = startsWith(content, 0xD0, 0xCF, 0x11, 0xE0);          // old Excel (.xls)
	        boolean pdf = startsWith(content, 0x25, 0x50, 0x44, 0x46);          // "%PDF"

	        if (pdf) {
	            throw new InvalidFileException(
	                    "This file is a PDF, which is not supported yet. Please upload a CSV or Excel file.");
	        }
	        switch (type) {
	            case XLSX -> {
	                if (!zip) {
	                    throw new InvalidFileException("The file content does not match the .xlsx extension");
	                }
	            }
	            case XLS -> {
	                if (!ole) {
	                    throw new InvalidFileException("The file content does not match the .xls extension. "
	                            + "Open it in Excel and save it as .xlsx or CSV.");
	                }
	            }
	            case CSV -> {
	                if (zip || ole || containsNul(content)) {
	                    throw new InvalidFileException("The file is not a plain-text CSV. "
	                            + "Please save it as CSV (UTF-8) and upload again.");
	                }
	            }
	        }
	    }

	    private static boolean startsWith(byte[] content, int... prefix) {
	        if (content.length < prefix.length) {
	            return false;
	        }
	        for (int i = 0; i < prefix.length; i++) {
	            if ((content[i] & 0xFF) != prefix[i]) {
	                return false;
	            }
	        }
	        return true;
	    }

	    private static boolean containsNul(byte[] content) {
	        int limit = Math.min(content.length, SNIFF_BYTES);
	        for (int i = 0; i < limit; i++) {
	            if (content[i] == 0) {
	                return true;
	            }
	        }
	        return false;
	    }
}
