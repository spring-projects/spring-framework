/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.reactive.server.assertj;


import org.springframework.test.web.reactive.server.ExchangeResult;

/**
 * Default implementation of {@link WebTestClientResponse}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
final class DefaultWebTestClientResponse implements WebTestClientResponse {

	private final ExchangeResult exchangeResult;


	DefaultWebTestClientResponse(ExchangeResult exchangeResult) {
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
	public WebTestClientResponseAssert assertThat() {
		return new WebTestClientResponseAssert(this);
	}

}
