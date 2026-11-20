/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;

/**
 * @author Juergen Hoeller
 */
class InterceptingStreamingHttpComponentsTests extends AbstractHttpRequestFactoryTests {

	@Override
	protected ClientHttpRequestFactory createRequestFactory() {
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		return new InterceptingClientHttpRequestFactory(requestFactory, null);
	}

	@Override
	@Test
	void httpMethods() throws Exception {
		super.httpMethods();
		assertHttpMethod("patch", HttpMethod.PATCH);
	}

}
