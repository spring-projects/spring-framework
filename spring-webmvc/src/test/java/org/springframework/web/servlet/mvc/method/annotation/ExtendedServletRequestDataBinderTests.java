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

package org.springframework.web.servlet.mvc.method.annotation;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.TestBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Test fixture for {@link ExtendedServletRequestDataBinder}.
 *
 * @author Rossen Stoyanchev
 */
public class ExtendedServletRequestDataBinderTests {

	private MockHttpServletRequest request;
	
	@Before
	public void setup() {
		this.request = new MockHttpServletRequest();
	}
	
	@Test
	public void createBinder() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<String, String>();
		uriTemplateVars.put("name", "nameValue");
		uriTemplateVars.put("age", "25");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);
		
		TestBean target = new TestBean();
		WebDataBinder binder = new ExtendedServletRequestDataBinder(target, "");
		((ServletRequestDataBinder) binder).bind(request);

		assertEquals("nameValue", target.getName());
		assertEquals(25, target.getAge());
	}

	@Test
	public void uriTemplateVarAndRequestParam() throws Exception {
		request.addParameter("age", "35");
		
		Map<String, String> uriTemplateVars = new HashMap<String, String>();
		uriTemplateVars.put("name", "nameValue");
		uriTemplateVars.put("age", "25");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);
		
		TestBean target = new TestBean();
		WebDataBinder binder = new ExtendedServletRequestDataBinder(target, "");
		((ServletRequestDataBinder) binder).bind(request);

		assertEquals("nameValue", target.getName());
		assertEquals(25, target.getAge());
	}

	@Test
	public void noUriTemplateVars() throws Exception {
		TestBean target = new TestBean();
		WebDataBinder binder = new ExtendedServletRequestDataBinder(target, "");
		((ServletRequestDataBinder) binder).bind(request);

		assertEquals(null, target.getName());
		assertEquals(0, target.getAge());
	}

}
