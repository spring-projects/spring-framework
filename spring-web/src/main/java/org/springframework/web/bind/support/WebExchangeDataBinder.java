/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.bind.support;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import reactor.core.publisher.Mono;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ServerWebExchange;

/**
 * Specialized {@link org.springframework.validation.DataBinder} to perform data
 * binding from URL query params or form data in the request data to Java objects.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class WebExchangeDataBinder extends WebDataBinder {

	/**
	 * Create a new instance, with default object name.
	 * @param target the target object to bind onto (or {@code null} if the
	 * binder is just used to convert a plain parameter value)
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public WebExchangeDataBinder(Object target) {
		super(target);
	}

	/**
	 * Create a new instance.
	 * @param target the target object to bind onto (or {@code null} if the
	 * binder is just used to convert a plain parameter value)
	 * @param objectName the name of the target object
	 */
	public WebExchangeDataBinder(Object target, String objectName) {
		super(target, objectName);
	}


	/**
	 * Bind the URL query parameters or form data of the body of the given request
	 * to this binder's target. The request body is parsed if the Content-Type
	 * is {@code "application/x-www-form-urlencoded"}.
	 * @param exchange the current exchange.
	 * @return a {@code Mono<Void>} when binding is complete
	 */
	public Mono<Void> bind(ServerWebExchange exchange) {
		return exchange.getRequestParams()
				.map(this::getParamsToBind)
				.doOnNext(values -> values.putAll(getMultipartFiles(exchange)))
				.doOnNext(values -> values.putAll(getExtraValuesToBind(exchange)))
				.then(values -> {
					doBind(new MutablePropertyValues(values));
					return Mono.empty();
				});
	}

	private Map<String, Object> getParamsToBind(MultiValueMap<String, String> params) {
		Map<String, Object> result = new TreeMap<>();
		for (Map.Entry<String, List<String>> entry : params.entrySet()) {
			String name = entry.getKey();
			List<String> values = entry.getValue();
			if (values == null || values.isEmpty()) {
				// Do nothing, no values found at all.
			}
			else {
				if (values.size() > 1) {
					result.put(name, values);
				}
				else {
					result.put(name, values.get(0));
				}
			}
		}
		return result;
	}

	/**
	 * Bind all multipart files contained in the given request, if any (in case
	 * of a multipart request).
	 * <p>Multipart files will only be added to the property values if they
	 * are not empty or if we're configured to bind empty multipart files too.
	 * @param exchange the current exchange
	 * @return Map of field name String to MultipartFile object
	 */
	protected Map<String, List<MultipartFile>> getMultipartFiles(ServerWebExchange exchange) {
		// TODO
		return Collections.emptyMap();
	}

	/**
	 * Extension point that subclasses can use to add extra bind values for a
	 * request. Invoked before {@link #doBind(MutablePropertyValues)}.
	 * The default implementation is empty.
	 * @param exchange the current exchange
	 */
	protected Map<String, ?> getExtraValuesToBind(ServerWebExchange exchange) {
		return Collections.emptyMap();
	}

}
