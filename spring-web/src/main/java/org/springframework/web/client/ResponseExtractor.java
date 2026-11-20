/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.client;

import java.io.IOException;
import java.lang.reflect.Type;

import org.jspecify.annotations.Nullable;

import org.springframework.http.client.ClientHttpResponse;

/**
 * Generic callback interface used by {@link RestTemplate}'s retrieval methods.
 * Implementations of this interface perform the actual work of extracting data
 * from a {@link ClientHttpResponse}, but don't need to worry about exception
 * handling or closing resources.
 *
 * <p>Used internally by the {@link RestTemplate}, but also useful for
 * application code. There is one available factory method, see
 * {@link RestTemplate#responseEntityExtractor(Type)}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @param <T> the data type
 * @see RestTemplate#execute
 * @deprecated as of 7.1 in favor of {@link RestClient.RequestBodySpec#exchange(RestClient.RequestHeadersSpec.ExchangeFunction)}.
 */
@FunctionalInterface
@Deprecated(since = "7.1", forRemoval = true)
@SuppressWarnings("removal")
public interface ResponseExtractor<T> {

	/**
	 * Extract data from the given {@code ClientHttpResponse} and return it.
	 * @param response the HTTP response
	 * @return the extracted data
	 * @throws IOException in case of I/O errors
	 */
	@Nullable T extractData(ClientHttpResponse response) throws IOException;

}
