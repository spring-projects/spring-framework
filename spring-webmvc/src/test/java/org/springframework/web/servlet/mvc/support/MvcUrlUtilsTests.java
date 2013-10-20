/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.mvc.support;

import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.support.MvcUrlUtils.ControllerMethodValues;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Test fixture for {@link MvcUrlUtils}.
 *
 * @author Oliver Gierke
 * @author Rossen Stoyanchev
 */
public class MvcUrlUtilsTests {

	private MockHttpServletRequest request;


	@Before
	public void setUp() {
		this.request = new MockHttpServletRequest();
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);
	}

	@After
	public void teardown() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	public void methodOn() {
		HttpEntity<Void> result = MvcUrlUtils.controller(SampleController.class).someMethod(1L);

		assertTrue(result instanceof ControllerMethodValues);
		assertEquals("someMethod", ((ControllerMethodValues) result).getControllerMethod().getName());
	}

	@Test
	public void typeLevelMapping() {
		assertThat(MvcUrlUtils.getTypeLevelMapping(MyController.class), is("/type"));
	}

	@Test
	public void typeLevelMappingNone() {
		assertThat(MvcUrlUtils.getTypeLevelMapping(ControllerWithoutTypeLevelMapping.class), is("/"));
	}

	@Test
	public void methodLevelMapping() throws Exception {
		Method method = MyController.class.getMethod("method");
		assertThat(MvcUrlUtils.getMethodMapping(method), is("/type/method"));
	}

	@Test
	public void methodLevelMappingWithoutTypeLevelMapping() throws Exception {
		Method method = ControllerWithoutTypeLevelMapping.class.getMethod("method");
		assertThat(MvcUrlUtils.getMethodMapping(method), is("/method"));
	}

	@Test
	public void methodMappingWithControllerMappingOnly() throws Exception {
		Method method = MyController.class.getMethod("noMethodMapping");
		assertThat(MvcUrlUtils.getMethodMapping(method), is("/type"));
	}


	@RequestMapping("/sample")
	static class SampleController {

		@RequestMapping("/{id}/foo")
		HttpEntity<Void> someMethod(@PathVariable("id") Long id) {
			return new ResponseEntity<Void>(HttpStatus.OK);
		}
	}

	@RequestMapping("/type")
	interface MyController {

		@RequestMapping("/method")
		void method();

		@RequestMapping
		void noMethodMapping();
	}

	interface ControllerWithoutTypeLevelMapping {

		@RequestMapping("/method")
		void method();
	}

}
