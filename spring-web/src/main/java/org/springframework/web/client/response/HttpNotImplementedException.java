package org.springframework.web.client.response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

import java.nio.charset.Charset;

public class HttpNotImplementedException extends HttpServerErrorException {

	private static final long serialVersionUID = -8858888941453536625L;

	public HttpNotImplementedException(HttpStatus statusCode) {
		super(statusCode);
	}

	public HttpNotImplementedException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	public HttpNotImplementedException(HttpStatus statusCode, String statusText, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseBody, responseCharset);
	}

	public HttpNotImplementedException(HttpStatus statusCode, String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseHeaders, responseBody, responseCharset);
	}
}
