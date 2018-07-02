package org.springframework.web.client.response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.Charset;

public class HttpMethodNotAllowedException extends HttpClientErrorException {

	private static final long serialVersionUID = -1485854208191929937L;

	public HttpMethodNotAllowedException(HttpStatus statusCode) {
		super(statusCode);
	}

	public HttpMethodNotAllowedException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	public HttpMethodNotAllowedException(HttpStatus statusCode, String statusText, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseBody, responseCharset);
	}

	public HttpMethodNotAllowedException(HttpStatus statusCode, String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseHeaders, responseBody, responseCharset);
	}
}
