package org.springframework.web.reactive.function.client;

import java.nio.charset.Charset;

import org.springframework.http.HttpHeaders;

/**
 * Exception thrown when an unknown (or custom) HTTP status code is received.
 *
 * @author Brian Clozel
 * @since 5.1
 */
public class UnknownHttpStatusCodeException extends WebClientResponseException {

	private static final long serialVersionUID = 2407169540168185007L;

	public UnknownHttpStatusCodeException(int statusCode, HttpHeaders headers,
			byte[] responseBody, Charset responseCharset) {
		super("Unknown status code [" + statusCode + "]", statusCode, "",
				headers, responseBody, responseCharset);
	}
	
}
