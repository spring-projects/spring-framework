/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.servlet.function;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UrlPathHelper;

/**
 * {@code ServerRequest} implementation based on a {@link HttpServletRequest}.
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
class DefaultServerRequest implements ServerRequest {

	private final ServletServerHttpRequest serverHttpRequest;

	private final Headers headers;

	private final List<HttpMessageConverter<?>> messageConverters;

	private final List<MediaType> allSupportedMediaTypes;

	private final MultiValueMap<String, String> params;

	private final Map<String, Object> attributes;


	public DefaultServerRequest(HttpServletRequest servletRequest,
			List<HttpMessageConverter<?>> messageConverters) {
		this.serverHttpRequest = new ServletServerHttpRequest(servletRequest);
		this.messageConverters = Collections.unmodifiableList(new ArrayList<>(messageConverters));
		this.allSupportedMediaTypes = allSupportedMediaTypes(messageConverters);

		this.headers = new DefaultRequestHeaders(this.serverHttpRequest.getHeaders());
		this.params = CollectionUtils.toMultiValueMap(new ServletParametersMap(servletRequest));
		this.attributes = new ServletAttributesMap(servletRequest);
	}

	private static List<MediaType> allSupportedMediaTypes(List<HttpMessageConverter<?>> messageConverters) {
		return messageConverters.stream()
				.flatMap(converter -> converter.getSupportedMediaTypes().stream())
				.sorted(MediaType.SPECIFICITY_COMPARATOR)
				.collect(Collectors.toList());
	}

	@Override
	public String methodName() {
		return servletRequest().getMethod();
	}

	@Override
	public URI uri() {
		return this.serverHttpRequest.getURI();
	}

	@Override
	public UriBuilder uriBuilder() {
		return ServletUriComponentsBuilder.fromRequest(servletRequest());
	}

	@Override
	public String path() {
		String path = (String) servletRequest().getAttribute(HandlerMapping.LOOKUP_PATH);
		if (path == null) {
			UrlPathHelper helper = new UrlPathHelper();
			path = helper.getLookupPathForRequest(servletRequest());
		}
		return path;
	}

	@Override
	public Headers headers() {
		return this.headers;
	}

	@Override
	public MultiValueMap<String, Cookie> cookies() {
		Cookie[] cookies = servletRequest().getCookies();
		if (cookies == null) {
			cookies = new Cookie[0];
		}
		MultiValueMap<String, Cookie> result = new LinkedMultiValueMap<>(cookies.length);
		for (Cookie cookie : cookies) {
			result.add(cookie.getName(), cookie);
		}
		return result;
	}

	@Override
	public HttpServletRequest servletRequest() {
		return this.serverHttpRequest.getServletRequest();
	}

	@Override
	public Optional<InetSocketAddress> remoteAddress() {
		return Optional.of(this.serverHttpRequest.getRemoteAddress());
	}

	@Override
	public List<HttpMessageConverter<?>> messageConverters() {
		return this.messageConverters;
	}

	@Override
	public <T> T body(Class<T> bodyType) throws IOException, ServletException {
		return bodyInternal(bodyType, bodyType);
	}

	@Override
	public <T> T body(ParameterizedTypeReference<T> bodyType) throws IOException, ServletException {
		Type type = bodyType.getType();
		return bodyInternal(type, bodyClass(type));
	}

	static Class<?> bodyClass(Type type) {
		if (type instanceof Class) {
			return (Class<?>) type;
		}
		if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			if (parameterizedType.getRawType() instanceof Class) {
				return (Class<?>) parameterizedType.getRawType();
			}
		}
		return Object.class;
	}

	@SuppressWarnings("unchecked")
	private <T> T bodyInternal(Type bodyType, Class<?> bodyClass)
			throws ServletException, IOException {

		MediaType contentType =
				this.headers.contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);

		for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
			if (messageConverter instanceof GenericHttpMessageConverter) {
				GenericHttpMessageConverter<T> genericMessageConverter =
						(GenericHttpMessageConverter<T>) messageConverter;
				if (genericMessageConverter.canRead(bodyType, bodyClass, contentType)) {
					return genericMessageConverter.read(bodyType, bodyClass, this.serverHttpRequest);
				}
			}
			if (messageConverter.canRead(bodyClass, contentType)) {
				HttpMessageConverter<T> theConverter =
						(HttpMessageConverter<T>) messageConverter;
				Class<? extends T> clazz = (Class<? extends T>) bodyClass;
				return theConverter.read(clazz, this.serverHttpRequest);
			}
		}
		throw new HttpMediaTypeNotSupportedException(contentType, this.allSupportedMediaTypes);
	}

	@Override
	public Optional<Object> attribute(String name) {
		return Optional.ofNullable(servletRequest().getAttribute(name));
	}

	@Override
	public Map<String, Object> attributes() {
		return this.attributes;
	}

	@Override
	public Optional<String> param(String name) {
		return Optional.ofNullable(servletRequest().getParameter(name));
	}

	@Override
	public MultiValueMap<String, String> params() {
		return this.params;
	}

	@Override
	public Map<String, String> pathVariables() {
		@SuppressWarnings("unchecked")
		Map<String, String> pathVariables = (Map<String, String>) servletRequest()
				.getAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		if (pathVariables != null) {
			return pathVariables;
		}
		else {
			return Collections.emptyMap();
		}
	}

	@Override
	public HttpSession session() {
		return servletRequest().getSession(true);
	}

	@Override
	public Optional<Principal> principal() {
		return Optional.ofNullable(this.serverHttpRequest.getPrincipal());
	}

	/**
	 * Default implementation of {@link Headers}.
	 */
	static class DefaultRequestHeaders implements Headers {

		private final HttpHeaders delegate;


		public DefaultRequestHeaders(HttpHeaders delegate) {
			this.delegate = delegate;
		}

		@Override
		public List<MediaType> accept() {
			return this.delegate.getAccept();
		}

		@Override
		public List<Charset> acceptCharset() {
			return this.delegate.getAcceptCharset();
		}

		@Override
		public List<Locale.LanguageRange> acceptLanguage() {
			return this.delegate.getAcceptLanguage();
		}

		@Override
		public OptionalLong contentLength() {
			long value = this.delegate.getContentLength();
			return (value != -1 ? OptionalLong.of(value) : OptionalLong.empty());
		}

		@Override
		public Optional<MediaType> contentType() {
			return Optional.ofNullable(this.delegate.getContentType());
		}

		@Override
		public InetSocketAddress host() {
			return this.delegate.getHost();
		}

		@Override
		public List<HttpRange> range() {
			return this.delegate.getRange();
		}

		@Override
		public List<String> header(String headerName) {
			List<String> headerValues = this.delegate.get(headerName);
			return (headerValues != null ? headerValues : Collections.emptyList());
		}

		@Override
		public HttpHeaders asHttpHeaders() {
			return HttpHeaders.readOnlyHttpHeaders(this.delegate);
		}

		@Override
		public String toString() {
			return this.delegate.toString();
		}
	}

	private static final class ServletParametersMap extends AbstractMap<String, List<String>> {

		private final HttpServletRequest servletRequest;


		private ServletParametersMap(HttpServletRequest servletRequest) {
			this.servletRequest = servletRequest;
		}

		@Override
		public Set<Entry<String, List<String>>> entrySet() {
			return this.servletRequest.getParameterMap().entrySet().stream()
					.map(entry -> {
						List<String> value = Arrays.asList(entry.getValue());
						return new SimpleImmutableEntry<>(entry.getKey(), value);
					})
					.collect(Collectors.toSet());
		}

		@Override
		public int size() {
			return this.servletRequest.getParameterMap().size();
		}

		@Override
		public List<String> get(Object key) {
			String name = (String) key;
			String[] parameterValues = this.servletRequest.getParameterValues(name);
			if (!ObjectUtils.isEmpty(parameterValues)) {
				return Arrays.asList(parameterValues);
			}
			else {
				return Collections.emptyList();
			}
		}

		@Override
		public List<String> put(String key, List<String> value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<String> remove(Object key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

	}


	private static final class ServletAttributesMap extends AbstractMap<String, Object> {

		private final HttpServletRequest servletRequest;

		private ServletAttributesMap(HttpServletRequest servletRequest) {
			this.servletRequest = servletRequest;
		}

		@Override
		public boolean containsKey(Object key) {
			String name = (String) key;
			return this.servletRequest.getAttribute(name) != null;
		}

		@Override
		public void clear() {
			List<String> attributeNames = Collections.list(this.servletRequest.getAttributeNames());
			attributeNames.forEach(this.servletRequest::removeAttribute);
		}

		@Override
		public Set<Entry<String, Object>> entrySet() {
			return Collections.list(this.servletRequest.getAttributeNames()).stream()
					.map(name -> {
						Object value = this.servletRequest.getAttribute(name);
						return new SimpleImmutableEntry<>(name, value);
					})
					.collect(Collectors.toSet());
		}

		@Override
		public Object get(Object key) {
			String name = (String) key;
			return this.servletRequest.getAttribute(name);
		}

		@Override
		public Object put(String key, Object value) {
			Object oldValue = this.servletRequest.getAttribute(key);
			this.servletRequest.setAttribute(key, value);
			return oldValue;
		}

		@Override
		public Object remove(Object key) {
			String name = (String) key;
			Object value = this.servletRequest.getAttribute(name);
			this.servletRequest.removeAttribute(name);
			return value;
		}


	}

}
