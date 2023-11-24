/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link ExtendedServletRequestDataBinder}.
 *
 * @author Rossen Stoyanchev
 */
public class ExtendedServletRequestDataBinderTests {

	private MockHttpServletRequest request;

	@BeforeEach
	public void setup() {
		this.request = new MockHttpServletRequest();
	}

	@Test
	public void createBinder() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("name", "nameValue");
		uriTemplateVars.put("age", "25");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		TestBean target = new TestBean();
		WebDataBinder binder = new ExtendedServletRequestDataBinder(target, "");
		((ServletRequestDataBinder) binder).bind(request);

		assertThat(target.getName()).isEqualTo("nameValue");
		assertThat(target.getAge()).isEqualTo(25);
	}

	@Test
	public void uriTemplateVarAndRequestParam() throws Exception {
		request.addParameter("age", "35");

		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("name", "nameValue");
		uriTemplateVars.put("age", "25");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		TestBean target = new TestBean();
		WebDataBinder binder = new ExtendedServletRequestDataBinder(target, "");
		((ServletRequestDataBinder) binder).bind(request);

		assertThat(target.getName()).isEqualTo("nameValue");
		assertThat(target.getAge()).isEqualTo(35);
	}

	@Test
	public void noUriTemplateVars() throws Exception {
		TestBean target = new TestBean();
		WebDataBinder binder = new ExtendedServletRequestDataBinder(target, "");
		((ServletRequestDataBinder) binder).bind(request);

		assertThat(target.getName()).isNull();
		assertThat(target.getAge()).isEqualTo(0);
	}

}
