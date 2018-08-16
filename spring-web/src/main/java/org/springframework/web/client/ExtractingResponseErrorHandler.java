/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.client;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * Implementation of {@link ResponseErrorHandler} that uses {@link HttpMessageConverter}s
 * to convert HTTP error responses to {@link RestClientException}.
 *
 * <p>To use this error handler, you must specify a
 * {@linkplain #setStatusMapping(Map) status mapping} and/or a
 * {@linkplain #setSeriesMapping(Map) series mapping}. If either of these mappings has a match
 * for the {@linkplain ClientHttpResponse#getStatusCode() status code} of a given
 * {@code ClientHttpResponse}, {@link #hasError(ClientHttpResponse)} will return
 * {@code true}, and {@link #handleError(ClientHttpResponse)} will attempt to use the
 * {@linkplain #setMessageConverters(List) configured message converters} to convert the response
 * into the mapped subclass of {@link RestClientException}. Note that the
 * {@linkplain #setStatusMapping(Map) status mapping} takes precedence over
 * {@linkplain #setSeriesMapping(Map) series mapping}.
 *
 * <p>If there is no match, this error handler will default to the behavior of
 * {@link DefaultResponseErrorHandler}. Note that you can override this default behavior
 * by specifying a {@linkplain #setSeriesMapping(Map) series mapping} from
 * {@code HttpStatus.Series#CLIENT_ERROR} and/or {@code HttpStatus.Series#SERVER_ERROR}
 * to {@code null}.
 *
 * @author Simon Galperin
 * @author Arjen Poutsma
 * @since 5.0
 * @see RestTemplate#setErrorHandler(ResponseErrorHandler)
 */
public class ExtractingResponseErrorHandler extends DefaultResponseErrorHandler {

	private List<HttpMessageConverter<?>> messageConverters = Collections.emptyList();

	private final Map<HttpStatus, Class<? extends RestClientException>> statusMapping = new LinkedHashMap<>();

	private final Map<HttpStatus.Series, Class<? extends RestClientException>> seriesMapping = new LinkedHashMap<>();


	/**
	 * Create a new, empty {@code ExtractingResponseErrorHandler}.
	 * <p>Note that {@link #setMessageConverters(List)} must be called when using this constructor.
	 */
	public ExtractingResponseErrorHandler() {
	}

	/**
	 * Create a new {@code ExtractingResponseErrorHandler} with the given
	 * {@link HttpMessageConverter} instances.
	 * @param messageConverters the message converters to use
	 */
	public ExtractingResponseErrorHandler(List<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters = messageConverters;
	}


	/**
	 * Set the message converters to use by this extractor.
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters = messageConverters;
	}

	/**
	 * Set the mapping from HTTP status code to {@code RestClientException} subclass.
	 * If this mapping has a match
	 * for the {@linkplain ClientHttpResponse#getStatusCode() status code} of a given
	 * {@code ClientHttpResponse}, {@link #hasError(ClientHttpResponse)} will return
	 * {@code true} and {@link #handleError(ClientHttpResponse)} will attempt to use the
	 * {@linkplain #setMessageConverters(List) configured message converters} to convert the
	 * response into the mapped subclass of {@link RestClientException}.
	 */
	public void setStatusMapping(Map<HttpStatus, Class<? extends RestClientException>> statusMapping) {
		if (!CollectionUtils.isEmpty(statusMapping)) {
			this.statusMapping.putAll(statusMapping);
		}
	}

	/**
	 * Set the mapping from HTTP status series to {@code RestClientException} subclass.
	 * If this mapping has a match
	 * for the {@linkplain ClientHttpResponse#getStatusCode() status code} of a given
	 * {@code ClientHttpResponse}, {@link #hasError(ClientHttpResponse)} will return
	 * {@code true} and {@link #handleError(ClientHttpResponse)} will attempt to use the
	 * {@linkplain #setMessageConverters(List) configured message converters} to convert the
	 * response into the mapped subclass of {@link RestClientException}.
	 */
	public void setSeriesMapping(Map<HttpStatus.Series, Class<? extends RestClientException>> seriesMapping) {
		if (!CollectionUtils.isEmpty(seriesMapping)) {
			this.seriesMapping.putAll(seriesMapping);
		}
	}


	@Override
	protected boolean hasError(HttpStatus statusCode) {
		if (this.statusMapping.containsKey(statusCode)) {
			return this.statusMapping.get(statusCode) != null;
		}
		else if (this.seriesMapping.containsKey(statusCode.series())) {
			return this.seriesMapping.get(statusCode.series()) != null;
		}
		else {
			return super.hasError(statusCode);
		}
	}

	@Override
	public void handleError(ClientHttpResponse response, HttpStatus statusCode) throws IOException {
		if (this.statusMapping.containsKey(statusCode)) {
			extract(this.statusMapping.get(statusCode), response);
		}
		else if (this.seriesMapping.containsKey(statusCode.series())) {
			extract(this.seriesMapping.get(statusCode.series()), response);
		}
		else {
			super.handleError(response, statusCode);
		}
	}

	private void extract(@Nullable Class<? extends RestClientException> exceptionClass,
			ClientHttpResponse response) throws IOException {

		if (exceptionClass == null) {
			return;
		}

		HttpMessageConverterExtractor<? extends RestClientException> extractor =
				new HttpMessageConverterExtractor<>(exceptionClass, this.messageConverters);
		RestClientException exception = extractor.extractData(response);
		if (exception != null) {
			throw exception;
		}
	}

}
