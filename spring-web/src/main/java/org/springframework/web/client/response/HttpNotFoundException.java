package org.springframework.web.client.response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.Charset;

public class HttpNotFoundException extends HttpClientErrorException {

	private static final long serialVersionUID = -9150078287238394669L;

	public HttpNotFoundException(HttpStatus statusCode) {
		super(statusCode);
	}

	public HttpNotFoundException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	public HttpNotFoundException(HttpStatus statusCode, String statusText, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseBody, responseCharset);
	}

	public HttpNotFoundException(HttpStatus statusCode, String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseHeaders, responseBody, responseCharset);
	}
}
