/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.servlet.view;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RedirectViewUriTemplateTests {

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	@BeforeEach
	public void setUp() {
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
		this.request.setAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
		this.request.setAttribute(DispatcherServlet.FLASH_MAP_MANAGER_ATTRIBUTE, new SessionFlashMapManager());
	}

	@Test
	void uriTemplate() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("foo", "bar");

		String baseUrl = "https://url.somewhere.com";
		RedirectView redirectView = new RedirectView(baseUrl + "/{foo}");
		redirectView.renderMergedOutputModel(model, this.request, this.response);

		assertThat(this.response.getRedirectedUrl()).isEqualTo((baseUrl + "/bar"));
	}

	@Test
	void uriTemplateEncode() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("foo", "bar/bar baz");

		String baseUrl = "https://url.somewhere.com";
		RedirectView redirectView = new RedirectView(baseUrl + "/context path/{foo}");
		redirectView.renderMergedOutputModel(model, this.request, this.response);

		assertThat(this.response.getRedirectedUrl()).isEqualTo((baseUrl + "/context path/bar%2Fbar%20baz"));
	}

	@Test
	void uriTemplateAndArrayQueryParam() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("foo", "bar");
		model.put("fooArr", new String[] { "baz", "bazz" });

		RedirectView redirectView = new RedirectView("/foo/{foo}");
		redirectView.renderMergedOutputModel(model, this.request, this.response);

		assertThat(this.response.getRedirectedUrl()).isEqualTo("/foo/bar?fooArr=baz&fooArr=bazz");
	}

	@Test
	void uriTemplateWithObjectConversion() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("foo", 611L);

		RedirectView redirectView = new RedirectView("/foo/{foo}");
		redirectView.renderMergedOutputModel(model, this.request, this.response);

		assertThat(this.response.getRedirectedUrl()).isEqualTo("/foo/611");
	}

	@Test
	void uriTemplateReuseCurrentRequestVars() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("key1", "value1");
		model.put("name", "value2");
		model.put("key3", "value3");

		Map<String, String> currentRequestUriTemplateVars = new HashMap<>();
		currentRequestUriTemplateVars.put("var1", "v1");
		currentRequestUriTemplateVars.put("name", "v2");
		currentRequestUriTemplateVars.put("var3", "v3");
		this.request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, currentRequestUriTemplateVars);

		String url = "https://url.somewhere.com";
		RedirectView redirectView = new RedirectView(url + "/{key1}/{var1}/{name}");
		redirectView.renderMergedOutputModel(model, this.request, this.response);

		assertThat(this.response.getRedirectedUrl()).isEqualTo((url + "/value1/v1/value2?key3=value3"));
	}

	@Test
	void uriTemplateNullValue() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new RedirectView("/{foo}").renderMergedOutputModel(new ModelMap(), this.request, this.response));
	}

	@Test
	void emptyRedirectString() throws Exception {
		Map<String, Object> model = new HashMap<>();

		RedirectView redirectView = new RedirectView("");
		redirectView.renderMergedOutputModel(model, this.request, this.response);

		assertThat(this.response.getRedirectedUrl()).isEmpty();
	}

	// SPR-9016

	@Test
	void dontApplyUriVariables() throws Exception {
		String url = "/test#{'one','abc'}";
		RedirectView redirectView = new RedirectView(url, true);
		redirectView.setExpandUriTemplateVariables(false);
		redirectView.renderMergedOutputModel(new ModelMap(), this.request, this.response);

		assertThat(this.response.getRedirectedUrl()).isEqualTo(url);
	}

}
