package org.springframework.web.client.response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.Charset;

public class HttpConflictException extends HttpClientErrorException {

	private static final long serialVersionUID = -147527825450228693L;

	public HttpConflictException(HttpStatus statusCode) {
		super(statusCode);
	}

	public HttpConflictException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	public HttpConflictException(HttpStatus statusCode, String statusText, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseBody, responseCharset);
	}

	public HttpConflictException(HttpStatus statusCode, String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseHeaders, responseBody, responseCharset);
	}
}
