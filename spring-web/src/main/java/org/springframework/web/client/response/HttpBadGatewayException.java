package org.springframework.web.client.response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

import java.nio.charset.Charset;

public class HttpBadGatewayException extends HttpServerErrorException {

	private static final long serialVersionUID = -8989300848677585487L;

	public HttpBadGatewayException(HttpStatus statusCode) {
		super(statusCode);
	}

	public HttpBadGatewayException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	public HttpBadGatewayException(HttpStatus statusCode, String statusText, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseBody, responseCharset);
	}

	public HttpBadGatewayException(HttpStatus statusCode, String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
		super(statusCode, statusText, responseHeaders, responseBody, responseCharset);
	}
}
