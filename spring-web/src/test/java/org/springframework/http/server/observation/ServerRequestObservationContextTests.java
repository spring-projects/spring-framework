/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.http.server.observation;


import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServerRequestObservationContext}.
 */
class ServerRequestObservationContextTests {

	private MockHttpServletRequest request = new MockHttpServletRequest();

	private MockHttpServletResponse response = new MockHttpServletResponse();

	private ServerRequestObservationContext context = new ServerRequestObservationContext(request, response);

	@Test
	void shouldGetMultipleHeaderValues() {
		request.addHeader("test", List.of("spring", "framework"));
		assertThat(context.getGetter().getAll(request, "test")).contains("spring", "framework");
	}

}
