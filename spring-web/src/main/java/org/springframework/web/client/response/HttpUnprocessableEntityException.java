package org.springframework.web.client.response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.Charset;

public class HttpUnprocessableEntityException extends HttpClientErrorException {

	private static final long serialVersionUID = 147931406869809016L;

	public HttpUnprocessableEntityException(HttpStatus statusCode) {
		super(statusCode);
	}

	public HttpUnprocessableEntityException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	public HttpUnprocessableEntityException(HttpStatus statusCode, String statusText, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseBody, responseCharset);
	}

	public HttpUnprocessableEntityException(HttpStatus statusCode, String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseHeaders, responseBody, responseCharset);
	}
}
