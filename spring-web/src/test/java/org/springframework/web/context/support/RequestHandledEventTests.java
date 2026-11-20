/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
