/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.client;

import org.springframework.http.HttpStatusCode;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;
import org.springframework.test.web.support.AbstractStatusAssertions;

/**
 * Assertions on the response status.
 *
 * @author Rob Worsnop
 * @since 7.0
 * @see ResponseSpec#expectStatus()
 */
public class StatusAssertions extends AbstractStatusAssertions<ExchangeResult, RestTestClient.ResponseSpec> {


	StatusAssertions(ExchangeResult exchangeResult, ResponseSpec responseSpec) {
		super(exchangeResult, responseSpec);
	}


	@Override
	protected HttpStatusCode getStatus() {
		return getExchangeResult().getStatus();
	}

	@Override
	protected void assertWithDiagnostics(Runnable assertion) {
		getExchangeResult().assertWithDiagnostics(assertion);
	}

}
