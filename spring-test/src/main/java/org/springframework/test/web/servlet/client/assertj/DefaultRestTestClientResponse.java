/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.client.assertj;

import org.springframework.test.web.servlet.client.ExchangeResult;

/**
 * Default implementation of {@link RestTestClientResponse}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
final class DefaultRestTestClientResponse implements RestTestClientResponse {

	private final ExchangeResult exchangeResult;


	DefaultRestTestClientResponse(ExchangeResult exchangeResult) {
		this.exchangeResult = exchangeResult;
	}


	@Override
	public ExchangeResult getExchangeResult() {
		return this.exchangeResult;
	}

	/**
	 * Use AssertJ's {@link org.assertj.core.api.Assertions#assertThat assertThat} instead.
	 */
	@Override
	public RestTestClientResponseAssert assertThat() {
		return new RestTestClientResponseAssert(this);
	}

}
