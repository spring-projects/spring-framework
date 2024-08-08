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

package org.springframework.web.service.invoker;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * Container for HTTP request values extracted from an
 * {@link org.springframework.web.service.annotation.HttpExchange @HttpExchange}-annotated
 * method and argument values passed to it. This is then given to
 * {@link HttpExchangeAdapter} to adapt to the underlying HTTP client.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 * @since 6.0
 */
public class HttpRequestValues {

	private static final MultiValueMap<String, String> EMPTY_COOKIES_MAP =
			CollectionUtils.toMultiValueMap(Collections.emptyMap());


	@Nullable
	private final HttpMethod httpMethod;

	@Nullable
	private final URI uri;

	@Nullable
	private final UriBuilderFactory uriBuilderFactory;

	@Nullable
	private final String uriTemplate;

	private final Map<String, String> uriVariables;

	private final HttpHeaders headers;

	private final MultiValueMap<String, String> cookies;

	private final Map<String, Object> attributes;

	@Nullable
	private final Object bodyValue;


	/**
	 * Construct {@link HttpRequestValues}.
	 * @since 6.1
	 */
	protected HttpRequestValues(@Nullable HttpMethod httpMethod,
			@Nullable URI uri, @Nullable UriBuilderFactory uriBuilderFactory,
			@Nullable String uriTemplate, Map<String, String> uriVariables,
			HttpHeaders headers, MultiValueMap<String, String> cookies, Map<String, Object> attributes,
			@Nullable Object bodyValue) {

		Assert.isTrue(uri != null || uriTemplate != null, "Neither URI nor URI template");

		this.httpMethod = httpMethod;
		this.uri = uri;
		this.uriBuilderFactory = uriBuilderFactory;
		this.uriTemplate = uriTemplate;
		this.uriVariables = uriVariables;
		this.headers = headers;
		this.cookies = cookies;
		this.attributes = attributes;
		this.bodyValue = bodyValue;
	}


	/**
	 * Return the HTTP method to use for the request.
	 */
	@Nullable
	public HttpMethod getHttpMethod() {
		return this.httpMethod;
	}

	/**
	 * Return the URL to use.
	 * <p>Typically, this comes from a {@link URI} method argument, which provides
	 * the caller with the option to override the {@link #getUriTemplate()
	 * uriTemplate} from class and method {@code HttpExchange} annotations.
	 */
	@Nullable
	public URI getUri() {
		return this.uri;
	}

	/**
	 * Return the {@link UriBuilderFactory} to expand
	 * the {@link HttpRequestValues#uriTemplate} and {@link #getUriVariables()} with.
	 * <p>The {@link UriBuilderFactory} is passed into the HTTP interface method
	 * in order to override the UriBuilderFactory (and its baseUrl) used by the
	 * underlying client.
	 * @since 6.1
	 */
	@Nullable
	public UriBuilderFactory getUriBuilderFactory() {
		return this.uriBuilderFactory;
	}

	/**
	 * Return the URL template for the request. This comes from the values in
	 * class and method {@code HttpExchange} annotations.
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
	 */
	@Nullable
	public Object getBodyValue() {
		return this.bodyValue;
	}


	public static Builder builder() {
		return new Builder();
	}


	/**
	 * Expose static metadata from {@code @HttpExchange} annotation attributes.
	 * @since 6.2
	 */
	public interface Metadata {

		/**
		 * Return the HTTP method, if known.
		 */
		@Nullable
		HttpMethod getHttpMethod();

		/**
		 * Return the URI template, if set already.
		 */
		@Nullable
		String getUriTemplate();

		/**
		 * Return the content type, if set already.
		 */
		@Nullable
		MediaType getContentType();

		/**
		 * Return the acceptable media types, if set already.
		 */
		@Nullable
		List<MediaType> getAcceptMediaTypes();
	}


	/**
	 * Builder for {@link HttpRequestValues}.
	 */
	public static class Builder implements Metadata {

		@Nullable
		private HttpMethod httpMethod;

		@Nullable
		private URI uri;

		@Nullable
		private UriBuilderFactory uriBuilderFactory;

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
		private MultiValueMap<String, Object> parts;

		@Nullable
		private Map<String, Object> attributes;

		@Nullable
		private Object bodyValue;

		/**
		 * Set the HTTP method for the request.
		 */
		public Builder setHttpMethod(HttpMethod httpMethod) {
			this.httpMethod = httpMethod;
			return this;
		}

		/**
		 * Set the URL to use. When set, this overrides the
		 * {@linkplain #setUriTemplate(String) URI template} from the
		 * {@code HttpExchange} annotation.
		 */
		public Builder setUri(URI uri) {
			this.uri = uri;
			return this;
		}

		/**
		 * Set the {@link UriBuilderFactory} that will be used to expand the
		 * {@link #getUriTemplate()}.
		 * @since 6.1
		 */
		public Builder setUriBuilderFactory(@Nullable UriBuilderFactory uriBuilderFactory) {
			this.uriBuilderFactory = uriBuilderFactory;
			return this;
		}

		/**
		 * Set the request URL as a String template.
		 */
		public Builder setUriTemplate(String uriTemplate) {
			this.uriTemplate = uriTemplate;
			return this;
		}

		/**
		 * Add a URI variable name-value pair.
		 */
		public Builder setUriVariable(String name, String value) {
			this.uriVars = (this.uriVars != null ? this.uriVars : new LinkedHashMap<>());
			this.uriVars.put(name, value);
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
		 * Add a part for a multipart request. The part may be:
		 * <ul>
		 * <li>String -- form field
		 * <li>{@link org.springframework.core.io.Resource Resource} -- file part
		 * <li>Object -- content to be encoded (e.g. to JSON)
		 * <li>{@link HttpEntity} -- part content and headers although generally it's
		 * easier to add headers through the returned builder
		 * </ul>
		 */
		public Builder addRequestPart(String name, Object part) {
			this.parts = (this.parts != null ? this.parts : new LinkedMultiValueMap<>());
			this.parts.add(name, part);
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
		 * Set the request body as an Object to be serialized.
		 */
		public void setBodyValue(@Nullable Object bodyValue) {
			this.bodyValue = bodyValue;
		}


		// Implementation of {@link Metadata} methods

		@Override
		@Nullable
		public HttpMethod getHttpMethod() {
			return this.httpMethod;
		}

		@Override
		@Nullable
		public String getUriTemplate() {
			return this.uriTemplate;
		}

		@Override
		@Nullable
		public MediaType getContentType() {
			return (this.headers != null ? this.headers.getContentType() : null);
		}

		@Override
		@Nullable
		public List<MediaType> getAcceptMediaTypes() {
			return (this.headers != null ? this.headers.getAccept() : null);
		}


		/**
		 * Build the {@link HttpRequestValues} instance.
		 */
		public HttpRequestValues build() {

			URI uri = this.uri;
			UriBuilderFactory uriBuilderFactory = this.uriBuilderFactory;
			String uriTemplate = (this.uriTemplate != null ? this.uriTemplate : "");
			Map<String, String> uriVars = (this.uriVars != null ? new HashMap<>(this.uriVars) : Collections.emptyMap());

			Object bodyValue = this.bodyValue;
			if (hasParts()) {
				Assert.isTrue(!hasBody(), "Expected body or request parts, not both");
				bodyValue = buildMultipartBody();
			}

			if (!CollectionUtils.isEmpty(this.requestParams)) {
				if (hasFormDataContentType()) {
					Assert.isTrue(!hasParts(), "Request parts not expected for a form data request");
					Assert.isTrue(!hasBody(), "Body not expected for a form data request");
					bodyValue = new LinkedMultiValueMap<>(this.requestParams);
				}
				else if (uri != null) {
					// insert into prepared URI
					uri = UriComponentsBuilder.fromUri(uri)
							.queryParams(UriUtils.encodeQueryParams(this.requestParams))
							.build(true)
							.toUri();
				}
				else {
					// append to URI template
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

			return createRequestValues(
					this.httpMethod, uri, uriBuilderFactory, uriTemplate, uriVars,
					headers, cookies, attributes, bodyValue);
		}

		protected boolean hasParts() {
			return (this.parts != null);
		}

		protected boolean hasBody() {
			return (this.bodyValue != null);
		}

		protected Object buildMultipartBody() {
			Assert.notNull(this.parts, "`parts` is null, was hasParts() not called?");
			return this.parts;
		}

		private boolean hasFormDataContentType() {
			return (this.headers != null &&
					MediaType.APPLICATION_FORM_URLENCODED.equals(this.headers.getContentType()));
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

		/**
		 * Create {@link HttpRequestValues} from values passed to the {@link Builder}.
		 * @since 6.1
		 */
		protected HttpRequestValues createRequestValues(
				@Nullable HttpMethod httpMethod,
				@Nullable URI uri, @Nullable UriBuilderFactory uriBuilderFactory, @Nullable String uriTemplate,
				Map<String, String> uriVars,
				HttpHeaders headers, MultiValueMap<String, String> cookies, Map<String, Object> attributes,
				@Nullable Object bodyValue) {

			return new HttpRequestValues(
					this.httpMethod, uri, uriBuilderFactory, uriTemplate,
					uriVars, headers, cookies, attributes, bodyValue);
		}
	}

}
