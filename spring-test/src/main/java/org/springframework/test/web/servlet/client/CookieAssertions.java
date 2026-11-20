/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.client;

import org.springframework.http.ResponseCookie;
import org.springframework.test.web.support.AbstractCookieAssertions;
import org.springframework.util.MultiValueMap;

/**
 * Assertions on cookies of the response.
 *
 * @author Rob Worsnop
 * @since 7.0
 */
public class CookieAssertions extends AbstractCookieAssertions<ExchangeResult, RestTestClient.ResponseSpec> {


	CookieAssertions(ExchangeResult exchangeResult, RestTestClient.ResponseSpec responseSpec) {
		super(exchangeResult, responseSpec);
	}


	@Override
	protected MultiValueMap<String, ResponseCookie> getResponseCookies() {
		return getExchangeResult().getResponseCookies();
	}

	@Override
	protected void assertWithDiagnostics(Runnable assertion) {
		getExchangeResult().assertWithDiagnostics(assertion);
	}

}
