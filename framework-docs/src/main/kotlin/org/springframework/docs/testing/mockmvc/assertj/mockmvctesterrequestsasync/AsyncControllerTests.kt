/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.testing.mockmvc.assertj.mockmvctesterrequestsasync

import org.assertj.core.api.Assertions.assertThat
import org.springframework.test.web.servlet.assertj.MockMvcTester
import java.time.Duration

class AsyncControllerTests {

	private val mockMvc = MockMvcTester.of(AsyncController())

	fun asyncExchangeWithCustomTimeToWait() {
		// tag::duration[]
		assertThat(mockMvc.get().uri("/compute").exchange(Duration.ofSeconds(5)))
			. // ...
			// end::duration[]
			hasStatusOk()
	}

	class AsyncController
}