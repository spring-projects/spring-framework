package org.springframework.web.client;

import java.net.URI;
import java.nio.charset.Charset;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;

/**
 * Strategy interface used by the {@link DefaultResponseErrorHandler} to compose
 * a summary of the http error.
 *
 * @author Jerzy Krolak
 * @since 5.1
 */
public interface HttpErrorDetailsExtractor {

	/**
	 * Assemble HTTP error response details string, based on the provided response details.
	 * @param rawStatusCode HTTP status code
	 * @param statusText HTTP status text
	 * @param responseBody response body
	 * @param responseCharset response charset
	 * @param url request URI
	 * @param method request method
	 * @return error details string
	 */
	@NotNull
	String getErrorDetails(int rawStatusCode, String statusText, @Nullable byte[] responseBody,
			@Nullable Charset responseCharset, @Nullable URI url, @Nullable HttpMethod method);

}
