/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Test fixture for {@link DefaultMvcResult}.
 *
 * @author Rossen Stoyanchev
 */
class DefaultMvcResultTests {

	private final DefaultMvcResult mvcResult = new DefaultMvcResult(new MockHttpServletRequest(), null);

	@Test
	void getAsyncResultSuccess() {
		this.mvcResult.setAsyncResult("Foo");
		this.mvcResult.setAsyncDispatchLatch(new CountDownLatch(0));
		this.mvcResult.getAsyncResult();
	}

	@Test
	void getAsyncResultFailure() {
		assertThatIllegalStateException().isThrownBy(() ->
				this.mvcResult.getAsyncResult(0));
	}

}
