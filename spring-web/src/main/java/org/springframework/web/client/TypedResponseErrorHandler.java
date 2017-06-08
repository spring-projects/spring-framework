/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;

/**
 * Spring's typed implementation of the {@link ResponseErrorHandler} interface.
 *
 * <p>
 * This error handler checks for the status code on the
 * {@link ClientHttpResponse}: Any code that matches HttpStatus provided in the
 * mapping, would be treated as failure.
 * 
 * Unmapped status codes provided in {@link HttpStatus.Series} would use
 * defaultType
 *
 * Default series (if not specified) would be
 * {@link HttpStatus.Series#CLIENT_ERROR} and
 * {@link HttpStatus.Series#SERVER_ERROR}.
 * 
 * In case {@link IOException} occurs during extraction, default behavior from
 * {@link DefaultResponseErrorHandler#handleError(ClientHttpResponse)}
 * 
 * @author Simon Galperin
 * @since 5.0
 * @see RestTemplate#setErrorHandler
 * @see DefaultResponseErrorHandler#handleError(ClientHttpResponse)
 */
public class TypedResponseErrorHandler extends DefaultResponseErrorHandler {
	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private final Map<HttpStatus, HttpMessageConverterExtractor<RestClientException>> responseExtractors = new HashMap<>();
	private final HttpMessageConverterExtractor<RestClientException> defaultExtractor;
	private final List<Series> series;

	public TypedResponseErrorHandler(List<HttpMessageConverter<?>> converters,
									 Class<? extends RestClientException> defaultType,
									 Series... series) {
		this(converters, Collections.emptyMap(), defaultType, series);
	}

	public TypedResponseErrorHandler(List<HttpMessageConverter<?>> converters,
									 Map<HttpStatus, Class<? extends RestClientException>> typeMap,
									 Series... series) {
		this(converters, typeMap, null, series);
	}

	public TypedResponseErrorHandler(List<HttpMessageConverter<?>> converters,
									 Map<HttpStatus, Class<? extends RestClientException>> typeMap,
									 Class<? extends RestClientException> defaultType,
									 Series... series) {
		typeMap.forEach((status, type) -> {
			this.responseExtractors.put(status,
										new HttpMessageConverterExtractor<RestClientException>(type, converters));
		});
		
		if (defaultType != null) {
			this.defaultExtractor = new HttpMessageConverterExtractor<RestClientException>(defaultType, converters);
		} else {
			this.defaultExtractor = null;
		}
		this.series = series.length > 0 ? Arrays.asList(series) : Arrays.asList(Series.CLIENT_ERROR, Series.SERVER_ERROR);
	}

	@Override
	protected boolean hasError(HttpStatus statusCode) {
		// if any of the status codes mapped, or the series
		return responseExtractors.keySet().contains(statusCode)
				|| series.contains(statusCode.series());
	}
	
	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		HttpStatus status = response.getStatusCode();

		HttpMessageConverterExtractor<RestClientException> responseExtractor = responseExtractors.get(status);

		if (responseExtractor == null && defaultExtractor != null) {
			responseExtractor = defaultExtractor;
		}

		if (responseExtractor != null) {
			try {
				RestClientException exception = responseExtractor.extractData(response);
				// if the body is empty, this object will be empty
				if (exception != null) {
					throw exception;
				}
			} catch (IOException ex) {
				logger.warn("Unable to extract error response for status " + status, ex);
			}
		}
		
		// default behavior in case we do not have specific error handling
		super.handleError(response);
	}
}