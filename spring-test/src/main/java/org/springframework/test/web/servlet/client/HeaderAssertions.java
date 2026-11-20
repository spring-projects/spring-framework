/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.client;

import org.springframework.http.HttpHeaders;
import org.springframework.test.web.support.AbstractHeaderAssertions;

/**
 * Assertions on headers of the response.
 *
 * @author Rob Worsnop
 * @since 7.0
 * @see RestTestClient.ResponseSpec#expectHeader()
 */
public class HeaderAssertions extends AbstractHeaderAssertions<ExchangeResult, RestTestClient.ResponseSpec> {


	HeaderAssertions(ExchangeResult exchangeResult, RestTestClient.ResponseSpec responseSpec) {
		super(exchangeResult, responseSpec);
	}


	@Override
	protected HttpHeaders getResponseHeaders() {
		return getExchangeResult().getResponseHeaders();
	}

	@Override
	protected void assertWithDiagnostics(Runnable assertion) {
		getExchangeResult().assertWithDiagnostics(assertion);
	}

}
