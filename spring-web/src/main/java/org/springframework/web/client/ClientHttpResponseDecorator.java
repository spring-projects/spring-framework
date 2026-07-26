/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.client;

import guru.mocker.annotation.mixin.Mixin;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;


/**
 * Wrap and delegate to an existing {@link ClientHttpResponse}.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
class ClientHttpResponseDecorator extends ClientHttpResponseDecoratorForwarder implements ClientHttpResponse {

	@Mixin
	public ClientHttpResponseDecorator(ClientHttpResponse delegate) {
		super(delegate);
		Assert.notNull(delegate, "ClientHttpResponse delegate is required");
	}

	/**
	 * Return the wrapped response.
	 */
	public ClientHttpResponse getDelegate() {
		return delegate;
	}

}
