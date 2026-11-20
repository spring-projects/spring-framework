/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.testing.resttestclient.assertj;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.ExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.assertj.RestTestClientResponse;

import static org.assertj.core.api.Assertions.assertThat;

public class AssertJTests {

	RestTestClient client;

	@Test
	void withSpec() {
		// tag::withSpec[]
		RestTestClient.ResponseSpec spec = client.get().uri("/persons").exchange();

		RestTestClientResponse response = RestTestClientResponse.from(spec);
		assertThat(response).hasStatusOk();
		assertThat(response).hasContentTypeCompatibleWith(MediaType.TEXT_PLAIN);
		// end::withSpec[]
	}

	@Test
	void withResult() {
		// tag::withResult[]
		ExchangeResult result = client.get().uri("/persons").exchange().returnResult();

		RestTestClientResponse response = RestTestClientResponse.from(result);
		assertThat(response).hasStatusOk();
		assertThat(response).hasContentTypeCompatibleWith(MediaType.TEXT_PLAIN);
		// end::withResult[]
	}

}
