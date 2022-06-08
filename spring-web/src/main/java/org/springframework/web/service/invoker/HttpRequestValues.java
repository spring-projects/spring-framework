/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.service.invoker;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.codec.FormHttpMessageWriter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * Container for HTTP request values extracted from an
 * {@link org.springframework.web.service.annotation.HttpExchange @HttpExchange}-annotated
 * method and argument values passed to it. This is then given to
 * {@link HttpClientAdapter} to adapt to the underlying HTTP client.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public final class HttpRequestValues {

	private static final MultiValueMap<String, String> EMPTY_COOKIES_MAP =
			CollectionUtils.toMultiValueMap(Collections.emptyMap());


	@Nullable
	private final HttpMethod httpMethod;

	@Nullable
	private final URI uri;

	@Nullable
	private final String uriTemplate;

	private final Map<String, String> uriVariables;

	private final HttpHeaders headers;

	private final MultiValueMap<String, String> cookies;

	private final Map<String, Object> attributes;

	@Nullable
	private final Object bodyValue;

	@Nullable
	private final Publisher<?> body;

	@Nullable
	private final ParameterizedTypeReference<?> bodyElementType;


	private HttpRequestValues(@Nullable HttpMethod httpMethod,
			@Nullable URI uri, @Nullable String uriTemplate, Map<String, String> uriVariables,
			HttpHeaders headers, MultiValueMap<String, String> cookies, Map<String, Object> attributes,
			@Nullable Object bodyValue,
			@Nullable Publisher<?> body, @Nullable ParameterizedTypeReference<?> bodyElementType) {

		Assert.isTrue(uri != null || uriTemplate != null, "Neither URI nor URI template");

		this.httpMethod = httpMethod;
		this.uri = uri;
		this.uriTemplate = uriTemplate;
		this.uriVariables = uriVariables;
		this.headers = headers;
		this.cookies = cookies;
		this.attributes = attributes;
		this.bodyValue = bodyValue;
		this.body = body;
		this.bodyElementType = bodyElementType;
	}


	/**
	 * Return the HTTP method to use for the request.
	 */
	@Nullable
	public HttpMethod getHttpMethod() {
		return this.httpMethod;
	}

	/**
	 * Return the full URL to use, if set.
	 * <p>This is mutually exclusive with {@link #getUriTemplate() uriTemplate}.
	 * One of the two has a value but not both.
	 */
	@Nullable
	public URI getUri() {
		return this.uri;
	}

	/**
	 * Return the URL template for the request, if set.
	 * <p>This is mutually exclusive with a {@linkplain #getUri() full URL}.
	 * One of the two has a value but not both.
	 */
	@Nullable
	public String getUriTemplate() {
		return this.uriTemplate;
	}

	/**
	 * Return the URL template variables, or an empty map.
	 */
	public Map<String, String> getUriVariables() {
		return this.uriVariables;
	}

	/**
	 * Return the headers for the request, if any.
	 */
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	/**
	 * Return the cookies for the request, or an empty map.
	 */
	public MultiValueMap<String, String> getCookies() {
		return this.cookies;
	}

	/**
	 * Return the attributes associated with the request, or an empty map.
	 */
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}

	/**
	 * Return the request body as a value to be serialized, if set.
	 * <p>This is mutually exclusive with {@link #getBody()}.
	 * Only one of the two or neither is set.
	 */
	@Nullable
	public Object getBodyValue() {
		return this.bodyValue;
	}

	/**
	 * Return the request body as a Publisher.
	 * <p>This is mutually exclusive with {@link #getBodyValue()}.
	 * Only one of the two or neither is set.
	 */
	@Nullable
	public Publisher<?> getBody() {
		return this.body;
	}

	/**
	 * Return the element type for a {@linkplain #getBody() Publisher body}.
	 */
	@Nullable
	public ParameterizedTypeReference<?> getBodyElementType() {
		return this.bodyElementType;
	}


	public static Builder builder() {
		return new Builder();
	}


	/**
	 * Builder for {@link HttpRequestValues}.
	 */
	public final static class Builder {

		private static final Function<MultiValueMap<String, String>, byte[]> FORM_DATA_SERIALIZER = new FormDataSerializer();

		@Nullable
		private HttpMethod httpMethod;

		@Nullable
		private URI uri;

		@Nullable
		private String uriTemplate;

		@Nullable
		private Map<String, String> uriVars;

		@Nullable
		private HttpHeaders headers;

		@Nullable
		private MultiValueMap<String, String> cookies;

		@Nullable
		private MultiValueMap<String, String> requestParams;

		@Nullable
		private Map<String, Object> attributes;

		@Nullable
		private Object bodyValue;

		@Nullable
		private Publisher<?> body;

		@Nullable
		private ParameterizedTypeReference<?> bodyElementType;

		/**
		 * Set the HTTP method for the request.
		 */
		public Builder setHttpMethod(HttpMethod httpMethod) {
			this.httpMethod = httpMethod;
			return this;
		}

		/**
		 * Set the request URL as a full URL.
		 * <p>This is mutually exclusive with, and resets any previously set
		 * {@linkplain #setUriTemplate(String) URI template} or
		 * {@linkplain #setUriVariable(String, String) URI variables}.
		 */
		public Builder setUri(URI uri) {
			this.uri = uri;
			this.uriTemplate = null;
			this.uriVars = null;
			return this;
		}

		/**
		 * Set the request URL as a String template.
		 * <p>This is mutually exclusive with, and resets any previously set
		 * {@linkplain #setUri(URI) full URI}.
		 */
		public Builder setUriTemplate(String uriTemplate) {
			this.uriTemplate = uriTemplate;
			this.uri = null;
			return this;
		}

		/**
		 * Add a URI variable name-value pair.
		 * <p>This is mutually exclusive with, and resets any previously set
		 * {@linkplain #setUri(URI) full URI}.
		 */
		public Builder setUriVariable(String name, String value) {
			this.uriVars = (this.uriVars != null ? this.uriVars : new LinkedHashMap<>());
			this.uriVars.put(name, value);
			this.uri = null;
			return this;
		}

		/**
		 * Set the media types for the request {@code Accept} header.
		 */
		public Builder setAccept(List<MediaType> acceptableMediaTypes) {
			initHeaders().setAccept(acceptableMediaTypes);
			return this;
		}

		/**
		 * Set the media type for the request {@code Content-Type} header.
		 */
		public Builder setContentType(MediaType contentType) {
			initHeaders().setContentType(contentType);
			return this;
		}

		/**
		 * Add the given header name and values.
		 */
		public Builder addHeader(String headerName, String... headerValues) {
			for (String headerValue : headerValues) {
				initHeaders().add(headerName, headerValue);
			}
			return this;
		}

		private HttpHeaders initHeaders() {
			this.headers = (this.headers != null ? this.headers : new HttpHeaders());
			return this.headers;
		}

		/**
		 * Add the given cookie name and values.
		 */
		public Builder addCookie(String name, String... values) {
			this.cookies = (this.cookies != null ? this.cookies : new LinkedMultiValueMap<>());
			for (String value : values) {
				this.cookies.add(name, value);
			}
			return this;
		}

		/**
		 * Add the given request parameter name and values.
		 * <p>When {@code "content-type"} is set to
		 * {@code "application/x-www-form-urlencoded"}, request parameters are
		 * encoded in the request body. Otherwise, they are added as URL query
		 * parameters.
		 */
		public Builder addRequestParameter(String name, String... values) {
			this.requestParams = (this.requestParams != null ? this.requestParams : new LinkedMultiValueMap<>());
			for (String value : values) {
				this.requestParams.add(name, value);
			}
			return this;
		}

		/**
		 * Configure an attribute to associate with the request.
		 * @param name the attribute name
		 * @param value the attribute value
		 */
		public Builder addAttribute(String name, Object value) {
			this.attributes = (this.attributes != null ? this.attributes : new HashMap<>());
			this.attributes.put(name, value);
			return this;
		}

		/**
		 * Set the request body as a concrete value to be serialized.
		 * <p>This is mutually exclusive with, and resets any previously set
		 * {@linkplain #setBody(Publisher, ParameterizedTypeReference) body Publisher}.
		 */
		public void setBodyValue(Object bodyValue) {
			this.bodyValue = bodyValue;
			this.body = null;
			this.bodyElementType = null;
		}

		/**
		 * Set the request body as a concrete value to be serialized.
		 * <p>This is mutually exclusive with, and resets any previously set
		 * {@linkplain #setBodyValue(Object) body value}.
		 */
		public <T, P extends Publisher<T>> void setBody(P body, ParameterizedTypeReference<T> elementTye) {
			this.body = body;
			this.bodyElementType = elementTye;
			this.bodyValue = null;
		}

		/**
		 * Builder the {@link HttpRequestValues} instance.
		 */
		public HttpRequestValues build() {

			URI uri = this.uri;
			String uriTemplate = (this.uriTemplate != null || uri != null ? this.uriTemplate : "");
			Map<String, String> uriVars = (this.uriVars != null ? new HashMap<>(this.uriVars) : Collections.emptyMap());

			Object bodyValue = this.bodyValue;

			if (!CollectionUtils.isEmpty(this.requestParams)) {

				boolean isFormData = (this.headers != null &&
						MediaType.APPLICATION_FORM_URLENCODED.equals(this.headers.getContentType()));

				if (isFormData) {
					Assert.isTrue(bodyValue == null && this.body == null, "Expected body or request params, not both");
					bodyValue = FORM_DATA_SERIALIZER.apply(this.requestParams);
				}
				else if (uri != null) {
					uri = UriComponentsBuilder.fromUri(uri)
							.queryParams(UriUtils.encodeQueryParams(this.requestParams))
							.build(true)
							.toUri();
				}
				else {
					uriVars = (uriVars.isEmpty() ? new HashMap<>() : uriVars);
					uriTemplate = appendQueryParams(uriTemplate, uriVars, this.requestParams);
				}
			}

			HttpHeaders headers = HttpHeaders.EMPTY;
			if (this.headers != null) {
				headers = new HttpHeaders();
				headers.putAll(this.headers);
			}

			MultiValueMap<String, String> cookies = (this.cookies != null ?
					new LinkedMultiValueMap<>(this.cookies) : EMPTY_COOKIES_MAP);

			Map<String, Object> attributes = (this.attributes != null ?
					new HashMap<>(this.attributes) : Collections.emptyMap());

			return new HttpRequestValues(
					this.httpMethod, uri, uriTemplate, uriVars, headers, cookies, attributes,
					bodyValue, this.body, this.bodyElementType);
		}

		private String appendQueryParams(
				String uriTemplate, Map<String, String> uriVars, MultiValueMap<String, String> requestParams) {

			UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(uriTemplate);
			int i = 0;
			for (Map.Entry<String, List<String>> entry : requestParams.entrySet()) {
				String nameVar = "queryParam" + i;
				uriVars.put(nameVar, entry.getKey());
				for (int j = 0; j < entry.getValue().size(); j++) {
					String valueVar = nameVar + "[" + j + "]";
					uriVars.put(valueVar, entry.getValue().get(j));
					uriComponentsBuilder.queryParam("{" + nameVar + "}", "{" + valueVar + "}");
				}
				i++;
			}
			return uriComponentsBuilder.build().toUriString();
		}

	}


	private static class FormDataSerializer
			extends FormHttpMessageWriter implements Function<MultiValueMap<String, String>, byte[]> {

		@Override
		public byte[] apply(MultiValueMap<String, String> requestParams) {
			Charset charset = StandardCharsets.UTF_8;
			return serializeForm(requestParams, charset).getBytes(charset);
		}

	}

}
