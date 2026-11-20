/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
