package org.springframework.web.client.response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.nio.charset.Charset;

public class HttpInternalServerErrorException extends HttpServerErrorException {

	private static final long serialVersionUID = -9078091996219553426L;

	public HttpInternalServerErrorException(HttpStatus statusCode) {
		super(statusCode);
	}

	public HttpInternalServerErrorException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	public HttpInternalServerErrorException(HttpStatus statusCode, String statusText, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseBody, responseCharset);
	}

	public HttpInternalServerErrorException(HttpStatus statusCode, String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseHeaders, responseBody, responseCharset);
	}
}
