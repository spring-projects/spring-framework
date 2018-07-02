package org.springframework.web.client.response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

import java.nio.charset.Charset;

public class HttpServiceUnavailableException extends HttpServerErrorException {

	private static final long serialVersionUID = 8777931838369402139L;

	public HttpServiceUnavailableException(HttpStatus statusCode) {
		super(statusCode);
	}

	public HttpServiceUnavailableException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	public HttpServiceUnavailableException(HttpStatus statusCode, String statusText, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseBody, responseCharset);
	}

	public HttpServiceUnavailableException(HttpStatus statusCode, String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseHeaders, responseBody, responseCharset);
	}
}
