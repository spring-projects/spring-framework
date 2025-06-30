/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
