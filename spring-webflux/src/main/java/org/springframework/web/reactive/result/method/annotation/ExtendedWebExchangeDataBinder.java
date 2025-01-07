/*
 * Copyright 2002-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.result.method.annotation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;

/**
 * Extended variant of {@link WebExchangeDataBinder} that adds URI path variables
 * and request headers to the bind values map.
 *
 * <p>Note: This class has existed since 5.0, but only as a private class within
 * {@link org.springframework.web.reactive.BindingContext}.
 *
 * @author Rossen Stoyanchev
 * @since 6.2.1
 */
public class ExtendedWebExchangeDataBinder extends WebExchangeDataBinder {

	private static final Set<String> FILTERED_HEADER_NAMES = Set.of("Accept", "Authorization", "Connection",
			"Cookie", "From", "Host", "Origin", "Priority", "Range", "Referer", "Upgrade");


	private Predicate<String> headerPredicate = name -> !FILTERED_HEADER_NAMES.contains(name);


	public ExtendedWebExchangeDataBinder(@Nullable Object target, String objectName) {
		super(target, objectName);
	}


	/**
	 * Add a Predicate that filters the header names to use for data binding.
	 * Multiple predicates are combined with {@code AND}.
	 * @param headerPredicate the predicate to add
	 * @since 6.2.1
	 */
	public void addHeaderPredicate(Predicate<String> headerPredicate) {
		this.headerPredicate = this.headerPredicate.and(headerPredicate);
	}

	/**
	 * Set the Predicate that filters the header names to use for data binding.
	 * <p>Note that this method resets any previous predicates that may have been
	 * set, including headers excluded by default such as the RFC 9218 defined
	 * "Priority" header.
	 * @param headerPredicate the predicate to add
	 * @since 6.2.1
	 */
	public void setHeaderPredicate(Predicate<String> headerPredicate) {
		this.headerPredicate = headerPredicate;
	}


	@Override
	public Mono<Map<String, Object>> getValuesToBind(ServerWebExchange exchange) {
		return super.getValuesToBind(exchange).doOnNext(map -> {
			Map<String, String> vars = exchange.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
			if (!CollectionUtils.isEmpty(vars)) {
				vars.forEach((key, value) -> addValueIfNotPresent(map, "URI variable", key, value));
			}
			HttpHeaders headers = exchange.getRequest().getHeaders();
			for (Map.Entry<String, List<String>> entry : headers.headerSet()) {
				String name = entry.getKey();
				if (!this.headerPredicate.test(entry.getKey())) {
					continue;
				}
				List<String> values = entry.getValue();
				if (!CollectionUtils.isEmpty(values)) {
					// For constructor args with @BindParam mapped to the actual header name
					addValueIfNotPresent(map, "Header", name, (values.size() == 1 ? values.get(0) : values));
					// Also adapt to Java conventions for setters
					name = StringUtils.uncapitalize(entry.getKey().replace("-", ""));
					addValueIfNotPresent(map, "Header", name, (values.size() == 1 ? values.get(0) : values));
				}
			}
		});
	}

	private static void addValueIfNotPresent(
			Map<String, Object> map, String label, String name, @Nullable Object value) {

		if (value != null) {
			if (map.containsKey(name)) {
				if (logger.isDebugEnabled()) {
					logger.debug(label + " '" + name + "' overridden by request bind value.");
				}
			}
			else {
				map.put(name, value);
			}
		}
	}

}
