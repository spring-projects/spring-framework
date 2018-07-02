package org.springframework.web.client.response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.Charset;

public class HttpTooManyRequestsException extends HttpClientErrorException {

	private static final long serialVersionUID = -7180196215964324224L;

	public HttpTooManyRequestsException(HttpStatus statusCode) {
		super(statusCode);
	}

	public HttpTooManyRequestsException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	public HttpTooManyRequestsException(HttpStatus statusCode, String statusText, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseBody, responseCharset);
	}

	public HttpTooManyRequestsException(HttpStatus statusCode, String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseHeaders, responseBody, responseCharset);
	}
}
