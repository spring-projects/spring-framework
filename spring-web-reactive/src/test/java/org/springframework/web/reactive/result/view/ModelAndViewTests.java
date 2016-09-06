/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.result.view;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 */
public class ModelAndViewTests {

	@Test
	public void view() {
		View view = mock(View.class);
		ModelAndView<View> mav = ModelAndView.view(view).build();
		assertFalse(mav.isReference());
		assertTrue(mav.view().isPresent());
		assertFalse(mav.viewName().isPresent());
		assertEquals(view, mav.view().get());
	}

	@Test
	public void viewName() {
		String viewName = "foo";
		ModelAndView<String> mav = ModelAndView.viewName(viewName).build();
		assertTrue(mav.isReference());
		assertTrue(mav.viewName().isPresent());
		assertFalse(mav.view().isPresent());
		assertEquals(viewName, mav.viewName().get());
	}

	@Test
	public void modelAttribute() {
		ModelAndView<String> mav = ModelAndView.viewName("foo")
				.modelAttribute("foo", "bar").build();
		assertEquals(1, mav.model().size());
		assertTrue(mav.model().containsKey("foo"));
		assertEquals("bar", mav.model().get("foo"));
	}

	@Test
	public void modelAttributeNoName() {
		ModelAndView<String> mav = ModelAndView.viewName("foo")
				.modelAttribute(this).build();
		assertEquals(1, mav.model().size());
		assertTrue(mav.model().containsKey("modelAndViewTests"));
		assertEquals(this, mav.model().get("modelAndViewTests"));
	}

	@Test
	public void modelAttributes() {
		Map<String, Object> model = new HashMap<>();
		model.put("foo", "bar");
		model.put("baz", "qux");
		ModelAndView<String> mav = ModelAndView.viewName("foo")
				.modelAttributes(model).build();
		assertEquals(2, mav.model().size());
		assertTrue(mav.model().containsKey("foo"));
		assertEquals("bar", mav.model().get("foo"));
		assertTrue(mav.model().containsKey("baz"));
		assertEquals("qux", mav.model().get("baz"));
	}
}