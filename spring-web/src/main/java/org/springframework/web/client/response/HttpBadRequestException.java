package org.springframework.web.client.response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.io.Serializable;
import java.nio.charset.Charset;

public class HttpBadRequestException extends HttpClientErrorException implements Serializable {

	private static final long serialVersionUID = -2064198172908428197L;

	public HttpBadRequestException(HttpStatus statusCode) {
		super(statusCode);
	}

	public HttpBadRequestException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	public HttpBadRequestException(HttpStatus statusCode, String statusText, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseBody, responseCharset);
	}

	public HttpBadRequestException(HttpStatus statusCode, String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseHeaders, responseBody, responseCharset);
	}
}
