/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.client.builder.parse.model;

import java.util.Map;
import java.util.Optional;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;

public class InvocationDetails {

	private final RequestDetails requestDetails;
	private final MultiValueMap<String, String> queryParams;
	private final Map<String, String> pathVariables;
	private final Object requestBody;
	private final boolean methodReurnTypeIsResponseEntity;
	private final ParameterizedTypeReference<?> responseType;
	private final HttpHeaders headers;

	public InvocationDetails(
			final RequestDetails requestDetails,
			final MultiValueMap<String, String> queryParams,
			final Map<String, String> pathVariables,
			final Object requestBody,
			final boolean methodReurnTypeIsResponseEntity,
			final ParameterizedTypeReference<?> responseType,
			final HttpHeaders headers) {
		this.requestDetails = requestDetails;
		this.queryParams = queryParams;
		this.pathVariables = pathVariables;
		this.requestBody = requestBody;
		this.methodReurnTypeIsResponseEntity = methodReurnTypeIsResponseEntity;
		this.responseType = responseType;
		this.headers = headers;
	}

	public Map<String, String> getPathVariables() {
		return this.pathVariables;
	}

	public MultiValueMap<String, String> getQueryParams() {
		return this.queryParams;
	}

	public HttpHeaders getHeaders() {
		return this.headers;
	}

	public Optional<Object> findRequestBody() {
		return Optional.ofNullable(this.requestBody);
	}

	public RequestDetails getRequestDetails() {
		return this.requestDetails;
	}

	public ParameterizedTypeReference<?> getResponseType() {
		return this.responseType;
	}

	public boolean isMethodReurnTypeResponseEntity() {
		return this.methodReurnTypeIsResponseEntity;
	}

	@Override
	public String toString() {
		return "InvocationDetails [requestDetails="
				+ this.requestDetails
				+ ", queryParams="
				+ this.queryParams
				+ ", pathVariables="
				+ this.pathVariables
				+ ", requestBody="
				+ this.requestBody
				+ ", methodReurnTypeIsResponseEntity="
				+ this.methodReurnTypeIsResponseEntity
				+ ", responseType="
				+ this.responseType
				+ ", headers="
				+ this.headers
				+ "]";
	}
}
