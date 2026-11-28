/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Tests that verify that {@link MockMvc} binds the {@link RequestAttributes}
 * for a request to the {@link RequestContextHolder} only while performing that
 * request and restores the previous {@code RequestAttributes} afterwards, even
 * if the request fails.
 *
 * @author Raphael Vullriede
 * @since 7.1.1
 * @see org.springframework.test.web.servlet.samples.spr.CustomRequestAttributesRequestContextHolderTests
 */
class MockMvcRequestAttributesTests {

	private final RequestAttributes previousAttributes =
			new ServletWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse());


	@BeforeEach
	void setRequestAttributes() {
		RequestContextHolder.setRequestAttributes(this.previousAttributes);
	}

	@AfterEach
	void resetRequestAttributes() {
		RequestContextHolder.resetRequestAttributes();
	}


	@Test
	void requestAttributesAreBoundWhileDefaultResultActionsAreApplied() throws Exception {
		AtomicReference<RequestAttributes> boundAttributes = new AtomicReference<>();
		MockMvc mockMvc = standaloneSetup(new TestController())
				.alwaysDo(result -> boundAttributes.set(RequestContextHolder.getRequestAttributes()))
				.build();

		MvcResult mvcResult = mockMvc.perform(get("/")).andReturn();

		assertThat(boundAttributes.get()).isInstanceOfSatisfying(ServletRequestAttributes.class,
				attributes -> assertThat(attributes.getRequest()).isSameAs(mvcResult.getRequest()));
		assertThat(RequestContextHolder.getRequestAttributes()).isSameAs(this.previousAttributes);
	}

	@Test
	void previousRequestAttributesAreRestoredIfFilterFails() {
		MockMvc mockMvc = standaloneSetup(new TestController())
				.addFilters((request, response, chain) -> {
					throw new IllegalStateException("Filter failure");
				})
				.build();

		assertThatIllegalStateException().isThrownBy(() -> mockMvc.perform(get("/")));

		assertThat(RequestContextHolder.getRequestAttributes()).isSameAs(this.previousAttributes);
	}

	@Test
	void previousRequestAttributesAreRestoredIfHandlerFails() {
		MockMvc mockMvc = standaloneSetup(new TestController()).build();

		assertThatExceptionOfType(ServletException.class)
				.isThrownBy(() -> mockMvc.perform(get("/failure")));

		assertThat(RequestContextHolder.getRequestAttributes()).isSameAs(this.previousAttributes);
	}

	@Test
	void previousRequestAttributesAreRestoredIfDefaultResultActionFails() {
		MockMvc mockMvc = standaloneSetup(new TestController())
				.alwaysExpect(status().isNotFound())
				.build();

		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> mockMvc.perform(get("/")));

		assertThat(RequestContextHolder.getRequestAttributes()).isSameAs(this.previousAttributes);
	}

	@Test
	void requestAttributesAreResetIfRequestFailsWithoutPreviousRequestAttributes() {
		RequestContextHolder.resetRequestAttributes();
		MockMvc mockMvc = standaloneSetup(new TestController()).build();

		assertThatExceptionOfType(ServletException.class)
				.isThrownBy(() -> mockMvc.perform(get("/failure")));

		assertThat(RequestContextHolder.getRequestAttributes()).isNull();
	}


	@RestController
	static class TestController {

		@GetMapping("/")
		String handle() {
			return "OK";
		}

		@GetMapping("/failure")
		String fail() {
			throw new IllegalStateException("Handler failure");
		}
	}

}
