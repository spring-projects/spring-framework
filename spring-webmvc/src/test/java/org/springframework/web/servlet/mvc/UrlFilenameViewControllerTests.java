/*
 * Copyright 2002-2019 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.ui.ModelMap;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(mv.getViewName()).isEqualTo("index");
		assertThat(mv.getModel().isEmpty()).isTrue();
	}

	@Test
	public void withFilenamePlusExtension() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/index.html");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertThat(mv.getViewName()).isEqualTo("index");
		assertThat(mv.getModel().isEmpty()).isTrue();
	}

	@Test
	public void withFilenameAndMatrixVariables() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/index;a=A;b=B");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertThat(mv.getViewName()).isEqualTo("index");
		assertThat(mv.getModel().isEmpty()).isTrue();
	}

	@Test
	public void withPrefixAndSuffix() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		ctrl.setPrefix("mypre_");
		ctrl.setSuffix("_mysuf");
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/index.html");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertThat(mv.getViewName()).isEqualTo("mypre_index_mysuf");
		assertThat(mv.getModel().isEmpty()).isTrue();
	}

	@Test
	public void withPrefix() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		ctrl.setPrefix("mypre_");
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/index.html");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertThat(mv.getViewName()).isEqualTo("mypre_index");
		assertThat(mv.getModel().isEmpty()).isTrue();
	}

	@Test
	public void withSuffix() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		ctrl.setSuffix("_mysuf");
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/index.html");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertThat(mv.getViewName()).isEqualTo("index_mysuf");
		assertThat(mv.getModel().isEmpty()).isTrue();
	}

	@Test
	public void multiLevel() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/docs/cvs/commit.html");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertThat(mv.getViewName()).isEqualTo("docs/cvs/commit");
		assertThat(mv.getModel().isEmpty()).isTrue();
	}

	@Test
	public void multiLevelWithMapping() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/docs/cvs/commit.html");
		exposePathInMapping(request, "/docs/**");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertThat(mv.getViewName()).isEqualTo("cvs/commit");
		assertThat(mv.getModel().isEmpty()).isTrue();
	}

	@Test
	public void multiLevelMappingWithFallback() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/docs/cvs/commit.html");
		exposePathInMapping(request, "/docs/cvs/commit.html");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertThat(mv.getViewName()).isEqualTo("docs/cvs/commit");
		assertThat(mv.getModel().isEmpty()).isTrue();
	}

	@Test
	public void withContextMapping() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myapp/docs/cvs/commit.html");
		request.setContextPath("/myapp");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertThat(mv.getViewName()).isEqualTo("docs/cvs/commit");
		assertThat(mv.getModel().isEmpty()).isTrue();
	}

	@Test
	public void settingPrefixToNullCausesEmptyStringToBeUsed() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		ctrl.setPrefix(null);
		assertThat(ctrl.getPrefix()).as("For setPrefix(..) with null, the empty string must be used instead.").isNotNull();
		assertThat(ctrl.getPrefix()).as("For setPrefix(..) with null, the empty string must be used instead.").isEqualTo("");
	}

	@Test
	public void settingSuffixToNullCausesEmptyStringToBeUsed() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		ctrl.setSuffix(null);
		assertThat(ctrl.getSuffix()).as("For setPrefix(..) with null, the empty string must be used instead.").isNotNull();
		assertThat(ctrl.getSuffix()).as("For setPrefix(..) with null, the empty string must be used instead.").isEqualTo("");
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
		assertThat(mv.getViewName()).isEqualTo("products/view");
		assertThat(mv.getModel().isEmpty()).isTrue();
	}

	@Test
	public void withFlashAttributes() throws Exception {
		UrlFilenameViewController ctrl = new UrlFilenameViewController();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/index");
		request.setAttribute(DispatcherServlet.INPUT_FLASH_MAP_ATTRIBUTE, new ModelMap("name", "value"));
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = ctrl.handleRequest(request, response);
		assertThat(mv.getViewName()).isEqualTo("index");
		assertThat(mv.getModel().size()).isEqualTo(1);
		assertThat(mv.getModel().get("name")).isEqualTo("value");
	}

	private void exposePathInMapping(MockHttpServletRequest request, String mapping) {
		String pathInMapping = this.pathMatcher.extractPathWithinPattern(mapping, request.getRequestURI());
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, pathInMapping);
	}

}
