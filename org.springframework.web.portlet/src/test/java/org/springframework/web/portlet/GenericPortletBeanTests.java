/*
 * Copyright 2002-2006 the original author or authors.
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

import junit.framework.TestCase;

import org.springframework.mock.web.portlet.MockPortletConfig;
import org.springframework.mock.web.portlet.MockPortletContext;

/**
 * @author Mark Fisher
 */
public class GenericPortletBeanTests extends TestCase {

	public void testInitParameterSet() throws Exception {
		PortletContext portletContext = new MockPortletContext();
		MockPortletConfig portletConfig = new MockPortletConfig(portletContext);
		String testValue = "testValue";
		portletConfig.addInitParameter("testParam", testValue);
		TestPortletBean portletBean = new TestPortletBean();
		assertNull(portletBean.getTestParam());
		portletBean.init(portletConfig);
		assertNotNull(portletBean.getTestParam());
		assertEquals(testValue, portletBean.getTestParam());
	}
	
	public void testInitParameterNotSet() throws Exception {
		PortletContext portletContext = new MockPortletContext();
		MockPortletConfig portletConfig = new MockPortletConfig(portletContext);
		TestPortletBean portletBean = new TestPortletBean();
		assertNull(portletBean.getTestParam());
		portletBean.init(portletConfig);
		assertNull(portletBean.getTestParam());
	}

	public void testMultipleInitParametersSet() throws Exception {
		PortletContext portletContext = new MockPortletContext();
		MockPortletConfig portletConfig = new MockPortletConfig(portletContext);
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

	public void testMultipleInitParametersOnlyOneSet() throws Exception {
		PortletContext portletContext = new MockPortletContext();
		MockPortletConfig portletConfig = new MockPortletConfig(portletContext);
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

	public void testRequiredInitParameterSet() throws Exception {
		PortletContext portletContext = new MockPortletContext();
		MockPortletConfig portletConfig = new MockPortletConfig(portletContext);
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

	public void testRequiredInitParameterNotSet() throws Exception {
		PortletContext portletContext = new MockPortletContext();
		MockPortletConfig portletConfig = new MockPortletConfig(portletContext);
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
	
	public void testRequiredInitParameterNotSetOtherParameterNotSet() throws Exception {
		PortletContext portletContext = new MockPortletContext();
		MockPortletConfig portletConfig = new MockPortletConfig(portletContext);
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

	public void testUnknownRequiredInitParameter() throws Exception {
		PortletContext portletContext = new MockPortletContext();
		MockPortletConfig portletConfig = new MockPortletConfig(portletContext);
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
