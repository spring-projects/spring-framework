/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.view;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class RedirectViewUriTemplateTests {

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	@Before
	public void setUp() {
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
	}

	@Test
	public void pathVar() throws Exception {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("foo", "bar");

		String baseUrl = "http://url.somewhere.com";
		RedirectView redirectView = new RedirectView(baseUrl + "/{foo}");
		redirectView.renderMergedOutputModel(model, request, response);

		assertEquals(baseUrl + "/bar", response.getRedirectedUrl());
	}
	
	@Test
	public void pathVarAndArrayParam() throws Exception {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("foo", "bar");
		model.put("fooArr", new String[] { "baz", "bazz" });

		RedirectView redirectView = new RedirectView("/foo/{foo}");
		redirectView.renderMergedOutputModel(model, request, response);

		assertEquals("/foo/bar?fooArr=baz&fooArr=bazz", response.getRedirectedUrl());
	}

	@Test
	public void pathVarWithObjectConversion() throws Exception {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("foo", new Long(611));

		RedirectView redirectView = new RedirectView("/foo/{foo}");
		redirectView.renderMergedOutputModel(model, request, response);

		assertEquals("/foo/611", response.getRedirectedUrl());
	}

}
