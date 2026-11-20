/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.client;

import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.test.web.support.AbstractXpathAssertions;
import org.springframework.util.Assert;

/**
 * XPath assertions for the {@link RestTestClient}.
 *
 * @author Rob Worsnop
 * @since 7.0
 */
public class XpathAssertions extends AbstractXpathAssertions<RestTestClient.BodyContentSpec> {


	XpathAssertions(
			RestTestClient.BodyContentSpec spec,
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
