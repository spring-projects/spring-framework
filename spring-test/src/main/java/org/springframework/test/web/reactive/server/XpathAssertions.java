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

package org.springframework.test.web.reactive.server;

import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.test.web.support.AbstractXpathAssertions;
import org.springframework.util.Assert;

/**
 * XPath assertions for the {@link WebTestClient}.
 *
 * @author Eric Deandrea
 * @author Rossen Stoyanchev
 * @author Rob Worsnop
 * @since 5.1
 */
public class XpathAssertions extends AbstractXpathAssertions<WebTestClient.BodyContentSpec> {


	XpathAssertions(
			WebTestClient.BodyContentSpec spec,
			String expression, @Nullable Map<String, String> namespaces, Object... args) {

		super(spec, expression, namespaces, args);
	}


	@Override
	protected Optional<HttpHeaders> getResponseHeaders() {
		return Optional.of(getBodySpec().returnResult()).map(ExchangeResult::getResponseHeaders);
	}

	@Override
	protected byte[] getContent() {
		byte[] body = getBodySpec().returnResult().getResponseBody();
		Assert.notNull(body, "Expected body content");
		return body;
	}
}
