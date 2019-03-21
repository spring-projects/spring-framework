/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.mvc.mapping;

import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class ControllerClassNameHandlerMappingTests {

	public static final String LOCATION = "/org/springframework/web/servlet/mvc/mapping/class-mapping.xml";

	private XmlWebApplicationContext wac;

	private HandlerMapping hm;

	private HandlerMapping hm2;

	private HandlerMapping hm3;

	private HandlerMapping hm4;


	@Before
	public void setUp() throws Exception {
		MockServletContext sc = new MockServletContext("");
		this.wac = new XmlWebApplicationContext();
		this.wac.setServletContext(sc);
		this.wac.setConfigLocations(new String[] {LOCATION});
		this.wac.refresh();
		this.hm = (HandlerMapping) this.wac.getBean("mapping");
		this.hm2 = (HandlerMapping) this.wac.getBean("mapping2");
		this.hm3 = (HandlerMapping) this.wac.getBean("mapping3");
		this.hm4 = (HandlerMapping) this.wac.getBean("mapping4");
	}

	@Test
	public void indexUri() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		HandlerExecutionChain chain = this.hm.getHandler(request);
		assertEquals(this.wac.getBean("index"), chain.getHandler());
	}

	@Test
	public void mapSimpleUri() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/welcome");
		HandlerExecutionChain chain = this.hm.getHandler(request);
		assertEquals(this.wac.getBean("welcome"), chain.getHandler());
	}

	@Test
	public void withContextPath() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myapp/welcome");
		request.setContextPath("/myapp");
		HandlerExecutionChain chain = this.hm.getHandler(request);
		assertEquals(this.wac.getBean("welcome"), chain.getHandler());
	}

	@Test
	public void withMultiActionControllerMapping() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/user");
		HandlerExecutionChain chain = this.hm.getHandler(request);
		assertEquals(this.wac.getBean("admin"), chain.getHandler());

		request = new MockHttpServletRequest("GET", "/admin/product");
		chain = this.hm.getHandler(request);
		assertEquals(this.wac.getBean("admin"), chain.getHandler());
	}

	@Test
	public void withoutControllerSuffix() throws Exception {
	  MockHttpServletRequest request = new MockHttpServletRequest("GET", "/buyform");
		HandlerExecutionChain chain = this.hm.getHandler(request);
		assertEquals(this.wac.getBean("buy"), chain.getHandler());
	}

	@Test
	public void withBasePackage() throws Exception {
	  MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myapp/mvc/mapping/welcome");
		HandlerExecutionChain chain = this.hm2.getHandler(request);
		assertEquals(this.wac.getBean("welcome"), chain.getHandler());
	}

	@Test
	public void withBasePackageAndCaseSensitive() throws Exception {
	  MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myapp/mvc/mapping/buyForm");
		HandlerExecutionChain chain = this.hm2.getHandler(request);
		assertEquals(this.wac.getBean("buy"), chain.getHandler());
	}

	@Test
	public void withFullBasePackage() throws Exception {
	  MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myapp/welcome");
		HandlerExecutionChain chain = this.hm3.getHandler(request);
		assertEquals(this.wac.getBean("welcome"), chain.getHandler());
	}

	@Test
	public void withRootAsBasePackage() throws Exception {
	  MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myapp/org/springframework/web/servlet/mvc/mapping/welcome");
		HandlerExecutionChain chain = this.hm4.getHandler(request);
		assertEquals(this.wac.getBean("welcome"), chain.getHandler());
	}

}
