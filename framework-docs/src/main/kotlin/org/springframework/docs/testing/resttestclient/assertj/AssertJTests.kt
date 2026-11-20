/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.testing.resttestclient.assertj

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.assertj.RestTestClientResponse

class AssertJTests {


	lateinit var client: RestTestClient

	@Test
	fun withSpec() {
		// tag::withSpec[]
		val spec = client.get().uri("/persons").exchange()

		val response = RestTestClientResponse.from(spec)
		Assertions.assertThat(response).hasStatusOk()
		Assertions.assertThat(response).hasContentTypeCompatibleWith(MediaType.TEXT_PLAIN)
		// end::withSpec[]
	}

	@Test
	fun withResult() {
		// tag::withResult[]
		val result = client.get().uri("/persons").exchange().returnResult()

		val response = RestTestClientResponse.from(result)
		Assertions.assertThat(response).hasStatusOk()
		Assertions.assertThat(response).hasContentTypeCompatibleWith(MediaType.TEXT_PLAIN)
		// end::withResult[]
	}

}
