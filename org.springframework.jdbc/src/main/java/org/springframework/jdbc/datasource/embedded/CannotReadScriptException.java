package org.springframework.jdbc.datasource.embedded;

import org.springframework.core.io.support.EncodedResource;

public class CannotReadScriptException extends RuntimeException {

	public CannotReadScriptException(EncodedResource resource, Throwable cause) {
		super("Cannot read SQL script from " + resource, cause);
	}
}
