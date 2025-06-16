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

package org.springframework.http.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@link ServerHttpRequest} implementation that is based on a {@link HttpServletRequest}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ServletServerHttpRequest implements ServerHttpRequest {

	protected static final Charset FORM_CHARSET = StandardCharsets.UTF_8;


	private final HttpServletRequest servletRequest;

	private @Nullable URI uri;

	private @Nullable HttpHeaders headers;

	private @Nullable Map<String, Object> attributes;


	private @Nullable ServerHttpAsyncRequestControl asyncRequestControl;


	/**
	 * Construct a new instance of the ServletServerHttpRequest based on the
	 * given {@link HttpServletRequest}.
	 * @param servletRequest the servlet request
	 */
	public ServletServerHttpRequest(HttpServletRequest servletRequest) {
		Assert.notNull(servletRequest, "HttpServletRequest must not be null");
		this.servletRequest = servletRequest;
	}


	/**
	 * Returns the {@code HttpServletRequest} this object is based on.
	 */
	public HttpServletRequest getServletRequest() {
		return this.servletRequest;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.servletRequest.getMethod());
	}

	@Override
	public URI getURI() {
		if (this.uri == null) {
			this.uri = initURI(this.servletRequest);
		}
		return this.uri;
	}

	/**
	 * Initialize a URI from the given Servlet request.
	 * @param servletRequest the request
	 * @return the initialized URI
	 * @since 6.1
	 */
	public static URI initURI(HttpServletRequest servletRequest) {
		String urlString = null;
		String query = null;
		boolean hasQuery = false;
		try {
			StringBuffer requestURL = servletRequest.getRequestURL();
			query = servletRequest.getQueryString();
			hasQuery = StringUtils.hasText(query);
			if (hasQuery) {
				requestURL.append('?').append(query);
			}
			urlString = requestURL.toString();
			return new URI(urlString);
		}
		catch (URISyntaxException ex) {
			if (hasQuery) {
				try {
					// Maybe malformed query, try to parse and encode it
					query = UriComponentsBuilder.fromUriString("?" + query).build().toUri().getRawQuery();
					return new URI(servletRequest.getRequestURL().toString() + "?" + query);
				}
				catch (URISyntaxException ex2) {
					try {
						// Try leaving it out
						return new URI(servletRequest.getRequestURL().toString());
					}
					catch (URISyntaxException ex3) {
						// ignore
					}
				}
			}
			throw new IllegalStateException(
					"Could not resolve HttpServletRequest as URI: " + urlString, ex);
		}
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			this.headers = new HttpHeaders();

			for (Enumeration<?> names = this.servletRequest.getHeaderNames(); names.hasMoreElements();) {
				String headerName = (String) names.nextElement();
				for (Enumeration<?> headerValues = this.servletRequest.getHeaders(headerName);
						headerValues.hasMoreElements();) {
					String headerValue = (String) headerValues.nextElement();
					this.headers.add(headerName, headerValue);
				}
			}

			// HttpServletRequest exposes some headers as properties:
			// we should include those if not already present
			try {
				MediaType contentType = this.headers.getContentType();
				if (contentType == null) {
					String requestContentType = this.servletRequest.getContentType();
					if (StringUtils.hasLength(requestContentType)) {
						contentType = MediaType.parseMediaType(requestContentType);
						if (contentType.isConcrete()) {
							this.headers.setContentType(contentType);
						}
					}
				}
				if (contentType != null && contentType.getCharset() == null) {
					String requestEncoding = this.servletRequest.getCharacterEncoding();
					if (StringUtils.hasLength(requestEncoding)) {
						Charset charSet = Charset.forName(requestEncoding);
						Map<String, String> params = new LinkedCaseInsensitiveMap<>();
						params.putAll(contentType.getParameters());
						params.put("charset", charSet.toString());
						MediaType mediaType = new MediaType(contentType.getType(), contentType.getSubtype(), params);
						this.headers.setContentType(mediaType);
					}
				}
			}
			catch (InvalidMediaTypeException ex) {
				// Ignore: simply not exposing an invalid content type in HttpHeaders...
			}

			if (this.headers.getContentLength() < 0) {
				int requestContentLength = this.servletRequest.getContentLength();
				if (requestContentLength != -1) {
					this.headers.setContentLength(requestContentLength);
				}
			}
		}

		return this.headers;
	}

	@Override
	public @Nullable Principal getPrincipal() {
		return this.servletRequest.getUserPrincipal();
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return new InetSocketAddress(this.servletRequest.getLocalAddr(), this.servletRequest.getLocalPort());
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return new InetSocketAddress(this.servletRequest.getRemoteHost(), this.servletRequest.getRemotePort());
	}

	@Override
	public Map<String, Object> getAttributes() {
		Map<String, Object> attributes = this.attributes;
		if (attributes == null) {
			attributes = new AttributesMap();
			this.attributes = attributes;
		}
		return attributes;
	}

	@Override
	public InputStream getBody() throws IOException {
		if (isFormPost(this.servletRequest) && this.servletRequest.getQueryString() == null) {
			return getBodyFromServletRequestParameters(this.servletRequest);
		}
		else {
			return this.servletRequest.getInputStream();
		}
	}

	@Override
	public ServerHttpAsyncRequestControl getAsyncRequestControl(ServerHttpResponse response) {
		if (this.asyncRequestControl == null) {
			if (!(response instanceof ServletServerHttpResponse servletServerResponse)) {
				throw new IllegalArgumentException(
						"Response must be a ServletServerHttpResponse: " + response.getClass());
			}
			this.asyncRequestControl = new ServletServerHttpAsyncRequestControl(this, servletServerResponse);
		}
		return this.asyncRequestControl;
	}


	private static boolean isFormPost(HttpServletRequest request) {
		String contentType = request.getContentType();
		return (contentType != null && contentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE) &&
				HttpMethod.POST.matches(request.getMethod()));
	}

	/**
	 * Use {@link jakarta.servlet.ServletRequest#getParameterMap()} to reconstruct the
	 * body of a form 'POST' providing a predictable outcome as opposed to reading
	 * from the body, which can fail if any other code has used the ServletRequest
	 * to access a parameter, thus causing the input stream to be "consumed".
	 */
	private InputStream getBodyFromServletRequestParameters(HttpServletRequest request) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		Charset charset = getFormCharset();
		Writer writer = new OutputStreamWriter(bos, charset);

		Map<String, String[]> form = request.getParameterMap();
		for (Iterator<Map.Entry<String, String[]>> entryItr = form.entrySet().iterator(); entryItr.hasNext();) {
			Map.Entry<String, String[]> entry = entryItr.next();
			List<String> values = Arrays.asList(entry.getValue());
			for (Iterator<String> valueItr = values.iterator(); valueItr.hasNext();) {
				String value = valueItr.next();
				writer.write(URLEncoder.encode(entry.getKey(), charset));
				if (value != null) {
					writer.write('=');
					writer.write(URLEncoder.encode(value, charset));
					if (valueItr.hasNext()) {
						writer.write('&');
					}
				}
			}
			if (entryItr.hasNext()) {
				writer.append('&');
			}
		}
		writer.flush();

		byte[] bytes = bos.toByteArray();
		if (bytes.length > 0 && getHeaders().containsHeader(HttpHeaders.CONTENT_LENGTH)) {
			getHeaders().setContentLength(bytes.length);
		}

		return new ByteArrayInputStream(bytes);
	}

	private Charset getFormCharset() {
		try {
			MediaType contentType = getHeaders().getContentType();
			if (contentType != null && contentType.getCharset() != null) {
				return contentType.getCharset();
			}
		}
		catch (Exception ex) {
			// ignore
		}
		return FORM_CHARSET;
	}


	private final class AttributesMap extends AbstractMap<String, Object> {

		private @Nullable transient Set<String> keySet;

		private @Nullable transient Collection<Object> values;

		private @Nullable transient Set<Entry<String, Object>> entrySet;


		@Override
		public int size() {
			int size = 0;
			for (Enumeration<?> names = servletRequest.getAttributeNames(); names.hasMoreElements(); names.nextElement()) {
				size++;
			}
			return size;
		}

		@Override
		public @Nullable Object get(Object key) {
			if (key instanceof String name) {
				return servletRequest.getAttribute(name);
			}
			else {
				return null;
			}
		}

		@Override
		public @Nullable Object put(String key, Object value) {
			Object old = get(key);
			servletRequest.setAttribute(key, value);
			return old;
		}

		@Override
		public @Nullable Object remove(Object key) {
			if (key instanceof String name) {
				Object old = get(key);
				servletRequest.removeAttribute(name);
				return old;
			}
			else {
				return null;
			}
		}

		@Override
		public void clear() {
			for (Enumeration<String> names = servletRequest.getAttributeNames(); names.hasMoreElements(); ) {
				String name = names.nextElement();
				servletRequest.removeAttribute(name);
			}
		}

		@Override
		public Set<String> keySet() {
			Set<String> keySet = this.keySet;
			if (keySet == null) {
				keySet = new AbstractSet<>() {
					@Override
					public Iterator<String> iterator() {
						return servletRequest.getAttributeNames().asIterator();
					}

					@Override
					public int size() {
						return AttributesMap.this.size();
					}
				};
				this.keySet = keySet;
			}
			return keySet;
		}

		@Override
		public Collection<Object> values() {
			Collection<Object> values = this.values;
			if (values == null) {
				values = new AbstractCollection<>() {
					@Override
					public Iterator<Object> iterator() {
						Enumeration<String> e = servletRequest.getAttributeNames();
						return new Iterator<>() {
							@Override
							public boolean hasNext() {
								return e.hasMoreElements();
							}

							@Override
							public Object next() {
								String name = e.nextElement();
								return servletRequest.getAttribute(name);
							}
						};
					}

					@Override
					public int size() {
						return AttributesMap.this.size();
					}
				};
				this.values = values;
			}
			return values;
		}

		@Override
		public Set<Entry<String, Object>> entrySet() {
			Set<Entry<String, Object>> entrySet = this.entrySet;
			if (entrySet == null) {
				entrySet = new AbstractSet<>() {
					@Override
					public Iterator<Entry<String, Object>> iterator() {
						Enumeration<String> e = servletRequest.getAttributeNames();
						return new Iterator<>() {
							@Override
							public boolean hasNext() {
								return e.hasMoreElements();
							}

							@Override
							public Entry<String, Object> next() {
								String name = e.nextElement();
								Object value = servletRequest.getAttribute(name);
								return new SimpleImmutableEntry<>(name, value);
							}
						};
					}

					@Override
					public int size() {
						return AttributesMap.this.size();
					}
				};
				this.entrySet = entrySet;
			}
			return entrySet;
		}
	}
}
