/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.testing.mockmvc.assertj.mockmvctesterrequestsasync;

import java.time.Duration;

import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

public class AsyncControllerTests {

	private final MockMvcTester mockMvc = MockMvcTester.of(new AsyncController());

	void asyncExchangeWithCustomTimeToWait() {
		// tag::duration[]
		assertThat(mockMvc.get().uri("/compute").exchange(Duration.ofSeconds(5)))
				. // ...
				// end::duration[]
				hasStatusOk();
	}

	static class AsyncController {}
}
