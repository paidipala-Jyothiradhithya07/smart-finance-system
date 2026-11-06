package com.smartfinance.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class SecurityErrorWriter {
	 private SecurityErrorWriter() {
	    }

	    public static void write(HttpServletRequest request, HttpServletResponse response,
	                             HttpStatus status, String message) throws IOException {
	        response.setStatus(status.value());
	        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
	        response.setCharacterEncoding("UTF-8");

	        String body = String.format(
	                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\",\"fieldErrors\":{}}",
	                Instant.now(), status.value(), status.getReasonPhrase(),
	                escape(message), escape(request.getRequestURI()));
	        response.getWriter().write(body);
	    }

	    private static String escape(String value) {
	        return value.replace("\\", "\\\\").replace("\"", "\\\"");
	    }
}
