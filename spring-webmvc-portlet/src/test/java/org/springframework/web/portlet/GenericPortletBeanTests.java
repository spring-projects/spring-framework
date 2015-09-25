/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.portlet;

import javax.portlet.PortletContext;
import javax.portlet.PortletException;

import org.junit.Test;

import org.springframework.mock.web.portlet.MockPortletConfig;
import org.springframework.mock.web.portlet.MockPortletContext;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 */
public class GenericPortletBeanTests {

	private final PortletContext portletContext = new MockPortletContext();

	private final MockPortletConfig portletConfig = new MockPortletConfig(portletContext);

	@Test
	public void initParameterSet() throws Exception {
		String testValue = "testValue";
		portletConfig.addInitParameter("testParam", testValue);
		TestPortletBean portletBean = new TestPortletBean();
		assertNull(portletBean.getTestParam());
		portletBean.init(portletConfig);
		assertNotNull(portletBean.getTestParam());
		assertEquals(testValue, portletBean.getTestParam());
	}

	@Test
	public void initParameterNotSet() throws Exception {
		TestPortletBean portletBean = new TestPortletBean();
		assertNull(portletBean.getTestParam());
		portletBean.init(portletConfig);
		assertNull(portletBean.getTestParam());
	}

	@Test
	public void multipleInitParametersSet() throws Exception {
		String testValue = "testValue";
		String anotherValue = "anotherValue";
		portletConfig.addInitParameter("testParam", testValue);
		portletConfig.addInitParameter("anotherParam", anotherValue);
		portletConfig.addInitParameter("unknownParam", "unknownValue");
		TestPortletBean portletBean = new TestPortletBean();
		assertNull(portletBean.getTestParam());
		assertNull(portletBean.getAnotherParam());
		portletBean.init(portletConfig);
		assertNotNull(portletBean.getTestParam());
		assertNotNull(portletBean.getAnotherParam());
		assertEquals(testValue, portletBean.getTestParam());
		assertEquals(anotherValue, portletBean.getAnotherParam());
	}

	@Test
	public void multipleInitParametersOnlyOneSet() throws Exception {
		String testValue = "testValue";
		portletConfig.addInitParameter("testParam", testValue);
		portletConfig.addInitParameter("unknownParam", "unknownValue");
		TestPortletBean portletBean = new TestPortletBean();
		assertNull(portletBean.getTestParam());
		assertNull(portletBean.getAnotherParam());
		portletBean.init(portletConfig);
		assertNotNull(portletBean.getTestParam());
		assertEquals(testValue, portletBean.getTestParam());
		assertNull(portletBean.getAnotherParam());
	}

	@Test
	public void requiredInitParameterSet() throws Exception {
		String testParam = "testParam";
		String testValue = "testValue";
		portletConfig.addInitParameter(testParam, testValue);
		TestPortletBean portletBean = new TestPortletBean();
		portletBean.addRequiredProperty(testParam);
		assertNull(portletBean.getTestParam());
		portletBean.init(portletConfig);
		assertNotNull(portletBean.getTestParam());
		assertEquals(testValue, portletBean.getTestParam());
	}

	@Test
	public void requiredInitParameterNotSet() throws Exception {
		String testParam = "testParam";
		TestPortletBean portletBean = new TestPortletBean();
		portletBean.addRequiredProperty(testParam);
		assertNull(portletBean.getTestParam());
		try {
			portletBean.init(portletConfig);
			fail("should have thrown PortletException");
		}
		catch (PortletException ex) {
			// expected
		}
	}

	@Test
	public void requiredInitParameterNotSetOtherParameterNotSet() throws Exception {
		String testParam = "testParam";
		String testValue = "testValue";
		portletConfig.addInitParameter(testParam, testValue);
		TestPortletBean portletBean = new TestPortletBean();
		portletBean.addRequiredProperty("anotherParam");
		assertNull(portletBean.getTestParam());
		try {
			portletBean.init(portletConfig);
			fail("should have thrown PortletException");
		}
		catch (PortletException ex) {
			// expected
		}
		assertNull(portletBean.getTestParam());
	}

	@Test
	public void unknownRequiredInitParameter() throws Exception {
		String testParam = "testParam";
		String testValue = "testValue";
		portletConfig.addInitParameter(testParam, testValue);
		TestPortletBean portletBean = new TestPortletBean();
		portletBean.addRequiredProperty("unknownParam");
		assertNull(portletBean.getTestParam());
		try {
			portletBean.init(portletConfig);
			fail("should have thrown PortletException");
		}
		catch (PortletException ex) {
			// expected
		}
		assertNull(portletBean.getTestParam());
	}


	@SuppressWarnings("unused")
	private static class TestPortletBean extends GenericPortletBean {

		private String testParam;
		private String anotherParam;

		public void setTestParam(String value) {
			this.testParam = value;
		}

		public String getTestParam() {
			return this.testParam;
		}

		public void setAnotherParam(String value) {
			this.anotherParam = value;
		}

		public String getAnotherParam() {
			return this.anotherParam;
		}
	}

}
