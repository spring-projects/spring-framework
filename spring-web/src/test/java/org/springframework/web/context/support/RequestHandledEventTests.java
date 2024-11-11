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
 * Tests for {@link RequestHandledEvent}.
 *
 * @author Stephane Nicoll
 */
class RequestHandledEventTests {

	@Test
	void descriptionWithNullableFields() {
		RequestHandledEvent event = new RequestHandledEvent(this, null, null, 400, null);
		assertThat(event.getDescription()).isEqualTo("session=[null]; user=[null]; time=[400ms]");
	}

	@Test
	void descriptionWithoutFailure() {
		RequestHandledEvent event = new RequestHandledEvent(this, "123-456", "user", 400, null);
		assertThat(event.getDescription()).isEqualTo("session=[123-456]; user=[user]; time=[400ms]");
	}

	@Test
	void descriptionWithFailure() {
		RequestHandledEvent event = new RequestHandledEvent(this, "123-456", "user", 400,
				new IllegalStateException("Expected failure"));
		assertThat(event.getDescription()).isEqualTo(
				"session=[123-456]; user=[user]; time=[400ms]; failure=[java.lang.IllegalStateException: Expected failure]");
	}

}
