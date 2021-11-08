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

package org.springframework.web.client.builder.handler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.RequestEntity.BodyBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.builder.parse.InvocationParser;
import org.springframework.web.client.builder.parse.model.InvocationDetails;
import org.springframework.web.util.UriComponentsBuilder;

public class SpringRestTemplateClientInvocationHandler<T> implements InvocationHandler {

	private final String url;
	private final RestTemplate restTemplate;

	public SpringRestTemplateClientInvocationHandler(
			final String url, final RestTemplate restTemplate, final HttpHeaders headers) {
		this.url = url;
		this.restTemplate = restTemplate;
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args)
			throws Throwable {
		final InvocationDetails invocationDetails =
				InvocationParser.getInvocationDetails(proxy, method, args);
		return this.doRequest(invocationDetails);
	}

	private Object doRequest(final InvocationDetails invocationDetails) {
		final URI uri =
				UriComponentsBuilder.fromHttpUrl(this.url) //
						.path(invocationDetails.getRequestDetails().getRequestPath())
						.queryParams(invocationDetails.getQueryParams())
						.build(invocationDetails.getPathVariables());

		final BodyBuilder bodyBuilder =
				RequestEntity.method(invocationDetails.getRequestDetails().getRequestMethod(), uri);

		bodyBuilder.headers(invocationDetails.getHeaders());

		if (invocationDetails.getRequestDetails().findConsumes().isPresent()) {
			bodyBuilder.contentType(invocationDetails.getRequestDetails().findConsumes().get());
		}

		if (invocationDetails.getRequestDetails().findProduces().isPresent()) {
			bodyBuilder.accept(invocationDetails.getRequestDetails().findProduces().get());
		}

		RequestEntity<?> requestEntity;
		if (invocationDetails.findRequestBody().isPresent()) {
			requestEntity = bodyBuilder.body(invocationDetails.findRequestBody().get());
		}
		else {
			requestEntity = bodyBuilder.build();
		}

		if (invocationDetails.isMethodReurnTypeResponseEntity()) {
			return this.restTemplate.exchange(requestEntity, invocationDetails.getResponseType());
		}
		else {
			return this.restTemplate
					.exchange(requestEntity, invocationDetails.getResponseType())
					.getBody();
		}
	}
}
