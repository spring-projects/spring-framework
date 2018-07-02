package org.springframework.web.client.response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.Charset;

public class HttpUnauthorizedException extends HttpClientErrorException {

	private static final long serialVersionUID = 2770517013134530298L;

	public HttpUnauthorizedException(HttpStatus statusCode) {
		super(statusCode);
	}

	public HttpUnauthorizedException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	public HttpUnauthorizedException(HttpStatus statusCode, String statusText, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseBody, responseCharset);
	}

	public HttpUnauthorizedException(HttpStatus statusCode, String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseHeaders, responseBody, responseCharset);
	}
}
