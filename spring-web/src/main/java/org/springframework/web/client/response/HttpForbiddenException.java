package org.springframework.web.client.response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.Charset;

public class HttpForbiddenException extends HttpClientErrorException {

	private static final long serialVersionUID = 620402597011417919L;

	public HttpForbiddenException(HttpStatus statusCode) {
		super(statusCode);
	}

	public HttpForbiddenException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	public HttpForbiddenException(HttpStatus statusCode, String statusText, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseBody, responseCharset);
	}

	public HttpForbiddenException(HttpStatus statusCode, String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseHeaders, responseBody, responseCharset);
	}
}
