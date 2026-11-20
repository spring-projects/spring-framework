/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.samples.standalone.resultmatchers;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

/**
 * Examples of expectations on the controller type and controller method.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class HandlerAssertionTests {

	private final MockMvc mockMvc = standaloneSetup(new SimpleController()).alwaysExpect(status().isOk()).build();


	@Test
	void handlerType() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(handler().handlerType(SimpleController.class));
	}

	@Test
	void methodCallOnNonMock() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				this.mockMvc.perform(get("/")).andExpect(handler().methodCall("bogus")))
			.withMessageContaining("The supplied object [bogus] is not an instance of")
			.withMessageContaining(MvcUriComponentsBuilder.MethodInvocationInfo.class.getName())
			.withMessageContaining("Ensure that you invoke the handler method via MvcUriComponentsBuilder.on()");
	}

	@Test
	void methodCall() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(handler().methodCall(on(SimpleController.class).handle()));
	}

	@Test
	void methodName() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(handler().methodName("handle"));
	}

	@Test
	void methodNameMatchers() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(handler().methodName(equalTo("handle")));
		this.mockMvc.perform(get("/")).andExpect(handler().methodName(is(not("save"))));
	}

	@Test
	void method() throws Exception {
		Method method = SimpleController.class.getMethod("handle");
		this.mockMvc.perform(get("/")).andExpect(handler().method(method));
	}


	@RestController
	static class SimpleController {

		@RequestMapping("/")
		public ResponseEntity<Void> handle() {
			return ResponseEntity.ok().build();
		}
	}

}
