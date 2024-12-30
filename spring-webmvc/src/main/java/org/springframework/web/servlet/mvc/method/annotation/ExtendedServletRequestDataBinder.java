/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Subclass of {@link ServletRequestDataBinder} that adds URI template variables
 * to the values used for data binding.
 *
 * <p><strong>WARNING</strong>: Data binding can lead to security issues by exposing
 * parts of the object graph that are not meant to be accessed or modified by
 * external clients. Therefore the design and use of data binding should be considered
 * carefully with regard to security. For more details, please refer to the dedicated
 * sections on data binding for
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">Spring Web MVC</a> and
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">Spring WebFlux</a>
 * in the reference manual.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see ServletRequestDataBinder
 * @see HandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE
 */
public class ExtendedServletRequestDataBinder extends ServletRequestDataBinder {

	private static final Set<String> FILTERED_HEADER_NAMES = Set.of("Priority");


	private Predicate<String> headerPredicate = name -> !FILTERED_HEADER_NAMES.contains(name);


	/**
	 * Create a new instance, with default object name.
	 * @param target the target object to bind onto (or {@code null}
	 * if the binder is just used to convert a plain parameter value)
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public ExtendedServletRequestDataBinder(@Nullable Object target) {
		super(target);
	}

	/**
	 * Create a new instance.
	 * @param target the target object to bind onto (or {@code null}
	 * if the binder is just used to convert a plain parameter value)
	 * @param objectName the name of the target object
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public ExtendedServletRequestDataBinder(@Nullable Object target, String objectName) {
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
	protected ServletRequestValueResolver createValueResolver(ServletRequest request) {
		return new ExtendedServletRequestValueResolver(request, this);
	}

	/**
	 * Merge URI variables into the property values to use for data binding.
	 */
	@Override
	protected void addBindValues(MutablePropertyValues mpvs, ServletRequest request) {
		Map<String, String> uriVars = getUriVars(request);
		if (uriVars != null) {
			uriVars.forEach((name, value) -> addValueIfNotPresent(mpvs, "URI variable", name, value));
		}
		if (request instanceof HttpServletRequest httpRequest) {
			Enumeration<String> names = httpRequest.getHeaderNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				Object value = getHeaderValue(httpRequest, name);
				if (value != null) {
					name = StringUtils.uncapitalize(name.replace("-", ""));
					addValueIfNotPresent(mpvs, "Header", name, value);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static @Nullable Map<String, String> getUriVars(ServletRequest request) {
		return (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
	}

	private static void addValueIfNotPresent(MutablePropertyValues mpvs, String label, String name, Object value) {
		if (mpvs.contains(name)) {
			if (logger.isDebugEnabled()) {
				logger.debug(label + " '" + name + "' overridden by request bind value.");
			}
		}
		else {
			mpvs.addPropertyValue(name, value);
		}
	}

	private @Nullable Object getHeaderValue(HttpServletRequest request, String name) {
		if (!this.headerPredicate.test(name)) {
			return null;
		}

		Enumeration<String> valuesEnum = request.getHeaders(name);
		if (!valuesEnum.hasMoreElements()) {
			return null;
		}

		String value = valuesEnum.nextElement();
		if (!valuesEnum.hasMoreElements()) {
			return value;
		}

		List<Object> values = new ArrayList<>();
		values.add(value);
		while (valuesEnum.hasMoreElements()) {
			values.add(valuesEnum.nextElement());
		}
		return values;
	}


	/**
	 * Resolver of values that looks up URI path variables.
	 */
	private class ExtendedServletRequestValueResolver extends ServletRequestValueResolver {

		ExtendedServletRequestValueResolver(ServletRequest request, WebDataBinder dataBinder) {
			super(request, dataBinder);
		}

		@Override
		protected @Nullable Object getRequestParameter(String name, Class<?> type) {
			Object value = super.getRequestParameter(name, type);
			if (value == null) {
				Map<String, String> uriVars = getUriVars(getRequest());
				if (uriVars != null) {
					value = uriVars.get(name);
				}
				if (value == null && getRequest() instanceof HttpServletRequest httpServletRequest) {
					value = getHeaderValue(httpServletRequest, name);
				}
			}
			return value;
		}

		@Override
		protected Set<String> initParameterNames(ServletRequest request) {
			Set<String> set = super.initParameterNames(request);
			Map<String, String> uriVars = getUriVars(getRequest());
			if (uriVars != null) {
				set.addAll(uriVars.keySet());
			}
			if (request instanceof HttpServletRequest httpServletRequest) {
				Enumeration<String> enumeration = httpServletRequest.getHeaderNames();
				while (enumeration.hasMoreElements()) {
					String headerName = enumeration.nextElement();
					set.add(headerName.replaceAll("-", ""));
				}
			}
			return set;
		}
	}

}
