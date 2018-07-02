package org.springframework.web.client.response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.Charset;

public class HttpNotAcceptableException extends HttpClientErrorException {

	private static final long serialVersionUID = -1762209525396296759L;

	public HttpNotAcceptableException(HttpStatus statusCode) {
		super(statusCode);
	}

	public HttpNotAcceptableException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	public HttpNotAcceptableException(HttpStatus statusCode, String statusText, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseBody, responseCharset);
	}

	public HttpNotAcceptableException(HttpStatus statusCode, String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseHeaders, responseBody, responseCharset);
	}
}
