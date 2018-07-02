package org.springframework.web.client.response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.Charset;

public class HttpUnsupportedMediaTypeException extends HttpClientErrorException {

	private static final long serialVersionUID = -7894170475662610655L;

	public HttpUnsupportedMediaTypeException(HttpStatus statusCode) {
		super(statusCode);
	}

	public HttpUnsupportedMediaTypeException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	public HttpUnsupportedMediaTypeException(HttpStatus statusCode, String statusText, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseBody, responseCharset);
	}

	public HttpUnsupportedMediaTypeException(HttpStatus statusCode, String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseHeaders, responseBody, responseCharset);
	}
}
