/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.context.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServletRequestHandledEvent}.
 *
 * @author Stephane Nicoll
 */
class ServletRequestHandledEventTests {

	@Test
	void descriptionWithoutStatusCode() {
		ServletRequestHandledEvent event = new ServletRequestHandledEvent(this,
				"/", "example.com", "GET", "dispatcher", "123-456", "user", 400);
		assertThat(event.getDescription()).contains("status=[-1]")
				.doesNotContain("failure=[");
	}

	@Test
	void descriptionWithFailure() {
		ServletRequestHandledEvent event = new ServletRequestHandledEvent(this,
				"/", "example.com", "GET", "dispatcher", "123-456", "user",
				400, new IllegalStateException("Test"), 500);
		assertThat(event.getDescription()).contains("status=[500]",
				"failure=[java.lang.IllegalStateException: Test]");
	}

}
