/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.web.context.support;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author Rob Winch
 */
public class WebApplicationContextUtilsTests {
	private MockServletContext sc;

	private String attrName;
	private String attrName2;

	private AnnotationConfigWebApplicationContext rootContext;
	private AnnotationConfigWebApplicationContext context;
	private AnnotationConfigWebApplicationContext context2;

	@Before
	public void setup() {
		sc = new MockServletContext();
		attrName = "attrName";
		attrName2 = "attrName2";
		rootContext = new AnnotationConfigWebApplicationContext();
		context = new AnnotationConfigWebApplicationContext();
		context2 = new AnnotationConfigWebApplicationContext();
	}

	@Test(expected = IllegalArgumentException.class)
	public void registerWebApplicationContextNullSc() {
		WebApplicationContextUtils.registerWebApplicationContext(null, attrName, context);
	}

	@Test(expected = IllegalArgumentException.class)
	public void registerWebApplicationContextNullAttrName() {
		WebApplicationContextUtils.registerWebApplicationContext(sc, null, context);
	}

	@Test(expected = IllegalArgumentException.class)
	public void registerWebApplicationContextNullContext() {
		WebApplicationContextUtils.registerWebApplicationContext(sc, attrName, null);
	}

	@Test
	public void registerWebApplicationContextNoRoot() {
		WebApplicationContextUtils.registerWebApplicationContext(sc, attrName, context);

		assertEquals(Arrays.asList(context),
				WebApplicationContextUtils.getRegisteredWebApplicationContexts(sc));
	}

	@Test
	public void registerWebApplicationContextWithRoot() {
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, rootContext);
		WebApplicationContextUtils.registerWebApplicationContext(sc, attrName, context);

		assertEquals(Arrays.asList(rootContext, context),
				WebApplicationContextUtils.getRegisteredWebApplicationContexts(sc));
	}

	@Test
	public void registerWebApplicationContextMultiChild() {
		WebApplicationContextUtils.registerWebApplicationContext(sc, attrName, context);
		WebApplicationContextUtils.registerWebApplicationContext(sc, attrName2, context2);
		sc.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, rootContext);

		assertEquals(Arrays.asList(rootContext, context, context2),
				WebApplicationContextUtils.getRegisteredWebApplicationContexts(sc));
	}

	@Test
	public void registerWebApplicationContextMultiWithSameAttr() {
		WebApplicationContextUtils.registerWebApplicationContext(sc, attrName, context);
		WebApplicationContextUtils.registerWebApplicationContext(sc, attrName, context2);

		assertEquals(Arrays.asList(context2),
				WebApplicationContextUtils.getRegisteredWebApplicationContexts(sc));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getRegisteredWebApplicationContextNull() {
		WebApplicationContextUtils.getRegisteredWebApplicationContexts(null);
	}

	@Test
	public void getRegisteredWebApplicationContext() {
		assertEquals(Collections.emptyList(),
				WebApplicationContextUtils.getRegisteredWebApplicationContexts(sc));
	}
}