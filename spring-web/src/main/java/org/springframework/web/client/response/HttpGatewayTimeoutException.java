package org.springframework.web.client.response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

import java.nio.charset.Charset;

public class HttpGatewayTimeoutException extends HttpServerErrorException {

	private static final long serialVersionUID = -7460116254256085095L;

	public HttpGatewayTimeoutException(HttpStatus statusCode) {
		super(statusCode);
	}

	public HttpGatewayTimeoutException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	public HttpGatewayTimeoutException(HttpStatus statusCode, String statusText, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseBody, responseCharset);
	}

	public HttpGatewayTimeoutException(HttpStatus statusCode, String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseHeaders, responseBody, responseCharset);
	}
}
