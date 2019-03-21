/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.mvc;

import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.ui.ModelMap;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 14.09.2005
 */
public class UrlFilenameViewControllerTests {

	private PathMatcher pathMatcher = new AntPathMatcher();


	@Test
	public void withPlainFilename() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/index");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertEquals("index", mv.getViewName());
		assertTrue(mv.getModel().isEmpty());
	}

	@Test
	public void withFilenamePlusExtension() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/index.html");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertEquals("index", mv.getViewName());
		assertTrue(mv.getModel().isEmpty());
	}

	@Test
	public void withFilenameAndMatrixVariables() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/index;a=A;b=B");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertEquals("index", mv.getViewName());
		assertTrue(mv.getModel().isEmpty());
	}

	@Test
	public void withPrefixAndSuffix() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		ctrl.setPrefix("mypre_");
		ctrl.setSuffix("_mysuf");
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/index.html");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertEquals("mypre_index_mysuf", mv.getViewName());
		assertTrue(mv.getModel().isEmpty());
	}

	@Test
	public void withPrefix() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		ctrl.setPrefix("mypre_");
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/index.html");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertEquals("mypre_index", mv.getViewName());
		assertTrue(mv.getModel().isEmpty());
	}

	@Test
	public void withSuffix() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		ctrl.setSuffix("_mysuf");
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/index.html");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertEquals("index_mysuf", mv.getViewName());
		assertTrue(mv.getModel().isEmpty());
	}

	@Test
	public void multiLevel() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/docs/cvs/commit.html");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertEquals("docs/cvs/commit", mv.getViewName());
		assertTrue(mv.getModel().isEmpty());
	}

	@Test
	public void multiLevelWithMapping() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/docs/cvs/commit.html");
		exposePathInMapping(request, "/docs/**");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertEquals("cvs/commit", mv.getViewName());
		assertTrue(mv.getModel().isEmpty());
	}

	@Test
	public void multiLevelMappingWithFallback() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/docs/cvs/commit.html");
		exposePathInMapping(request, "/docs/cvs/commit.html");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertEquals("docs/cvs/commit", mv.getViewName());
		assertTrue(mv.getModel().isEmpty());
	}

	@Test
	public void withContextMapping() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myapp/docs/cvs/commit.html");
		request.setContextPath("/myapp");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertEquals("docs/cvs/commit", mv.getViewName());
		assertTrue(mv.getModel().isEmpty());
	}

	@Test
	public void settingPrefixToNullCausesEmptyStringToBeUsed() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		ctrl.setPrefix(null);
		assertNotNull("For setPrefix(..) with null, the empty string must be used instead.", ctrl.getPrefix());
		assertEquals("For setPrefix(..) with null, the empty string must be used instead.", "", ctrl.getPrefix());
	}

	@Test
	public void settingSuffixToNullCausesEmptyStringToBeUsed() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		ctrl.setSuffix(null);
		assertNotNull("For setPrefix(..) with null, the empty string must be used instead.", ctrl.getSuffix());
		assertEquals("For setPrefix(..) with null, the empty string must be used instead.", "", ctrl.getSuffix());
	}

	/**
	 * This is the expected behavior, and it now has a test to prove it.
	 * https://opensource.atlassian.com/projects/spring/browse/SPR-2789
	 */
	@Test
	public void nestedPathisUsedAsViewName_InBreakingChangeFromSpring12Line() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/products/view.html");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertEquals("products/view", mv.getViewName());
		assertTrue(mv.getModel().isEmpty());
	}

	@Test
	public void withFlashAttributes() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/index");
		request.setAttribute(DispatcherServlet.INPUT_FLASH_MAP_ATTRIBUTE, new ModelMap("name", "value"));
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertEquals("index", mv.getViewName());
		assertEquals(1, mv.getModel().size());
		assertEquals("value", mv.getModel().get("name"));
	}

	private void exposePathInMapping(MockHttpServletRequest request, String mapping) {
		String pathInMapping = this.pathMatcher.extractPathWithinPattern(mapping, request.getRequestURI());
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, pathInMapping);
	}

}
