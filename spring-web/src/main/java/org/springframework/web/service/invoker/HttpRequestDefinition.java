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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;

import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;


/**
 * Container for HTTP request values accumulated from an
 * {@link HttpRequest @HttpRequest}-annotated method and arguments passed to it.
 * This allows an {@link HttpClientAdapter} adapt these inputs as it sees fit
 * to the API of the underlying client.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public class HttpRequestDefinition {

	private static final MultiValueMap<String, String> EMPTY_COOKIES_MAP =
			CollectionUtils.toMultiValueMap(Collections.emptyMap());


	@Nullable
	private URI uri;

	@Nullable
	private String uriTemplate;

	@Nullable
	private Map<String, String> uriVariables;

	@Nullable
	private List<String> uriVariablesList;

	@Nullable
	private HttpMethod httpMethod;

	@Nullable
	private HttpHeaders headers;

	@Nullable
	private MultiValueMap<String, String> cookies;

	@Nullable
	private Object bodyValue;

	@Nullable
	private Publisher<?> bodyPublisher;

	@Nullable
	private ParameterizedTypeReference<?> bodyPublisherElementType;

	private boolean complete;


	public void setUri(URI uri) {
		checkComplete();
		this.uri = uri;
	}

	@Nullable
	public URI getUri() {
		return this.uri;
	}

	public void setUriTemplate(String uriTemplate) {
		checkComplete();
		this.uriTemplate = uriTemplate;
	}

	@Nullable
	public String getUriTemplate() {
		return this.uriTemplate;
	}

	public Map<String, String> getUriVariables() {
		this.uriVariables = (this.uriVariables != null ? this.uriVariables : new LinkedHashMap<>());
		return this.uriVariables;
	}

	public List<String> getUriVariableValues() {
		this.uriVariablesList = (this.uriVariablesList != null ? this.uriVariablesList : new ArrayList<>());
		return this.uriVariablesList;
	}

	public void setHttpMethod(HttpMethod httpMethod) {
		checkComplete();
		this.httpMethod = httpMethod;
	}

	@Nullable
	public HttpMethod getHttpMethod() {
		return this.httpMethod;
	}

	public HttpMethod getHttpMethodRequired() {
		Assert.notNull(this.httpMethod, "No HttpMethod");
		return this.httpMethod;
	}

	public HttpHeaders getHeaders() {
		this.headers = (this.headers != null ? this.headers : new HttpHeaders());
		return this.headers;
	}

	public MultiValueMap<String, String> getCookies() {
		this.cookies = (this.cookies != null ? this.cookies : new LinkedMultiValueMap<>());
		return this.cookies;
	}

	public void setBodyValue(Object bodyValue) {
		checkComplete();
		this.bodyValue = bodyValue;
	}

	@Nullable
	public Object getBodyValue() {
		return this.bodyValue;
	}

	public <T, P extends Publisher<T>> void setBodyPublisher(Publisher<P> bodyPublisher, MethodParameter parameter) {
		checkComplete();
		// Adapt to Mono/Flux and nest MethodParameter for element type
		this.bodyPublisher = bodyPublisher;
		this.bodyPublisherElementType = ParameterizedTypeReference.forType(parameter.nested().getGenericParameterType());
	}

	@Nullable
	public Publisher<?> getBodyPublisher() {
		return this.bodyPublisher;
	}

	public ParameterizedTypeReference<?> getBodyPublisherElementType() {
		Assert.state(this.bodyPublisherElementType != null, "No body Publisher");
		return this.bodyPublisherElementType;
	}

	private void checkComplete() {
		Assert.isTrue(!this.complete, "setComplete already called");
	}


	void setComplete() {

		this.uriVariables = (this.uriVariables != null ?
				Collections.unmodifiableMap(this.uriVariables) : Collections.emptyMap());

		this.uriVariablesList = (this.uriVariablesList != null ?
				Collections.unmodifiableList(this.uriVariablesList) : Collections.emptyList());

		this.headers = (this.headers != null ?
				HttpHeaders.readOnlyHttpHeaders(this.headers) : HttpHeaders.EMPTY);

		this.cookies = (this.cookies != null ?
				CollectionUtils.unmodifiableMultiValueMap(this.cookies) : EMPTY_COOKIES_MAP);
	}

}
