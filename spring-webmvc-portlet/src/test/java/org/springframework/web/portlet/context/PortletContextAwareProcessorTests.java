/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.web.portlet.context;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;

import junit.framework.TestCase;

import org.springframework.mock.web.portlet.MockPortletConfig;
import org.springframework.mock.web.portlet.MockPortletContext;

/**
 * @author Mark Fisher
 */
public class PortletContextAwareProcessorTests extends TestCase {

	public void testPortletContextAwareWithPortletContext() {
		PortletContext portletContext = new MockPortletContext();
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor(portletContext);
		PortletContextAwareBean bean = new PortletContextAwareBean();
		assertNull(bean.getPortletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("PortletContext should have been set", bean.getPortletContext());
		assertEquals(portletContext, bean.getPortletContext());
	}

	public void testPortletContextAwareWithPortletConfig() {
		PortletContext portletContext = new MockPortletContext();
		PortletConfig portletConfig = new MockPortletConfig(portletContext);
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor(portletConfig);
		PortletContextAwareBean bean = new PortletContextAwareBean();
		assertNull(bean.getPortletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("PortletContext should have been set", bean.getPortletContext());
		assertEquals(portletContext, bean.getPortletContext());
	}

	public void testPortletContextAwareWithPortletContextAndPortletConfig() {
		PortletContext portletContext = new MockPortletContext();
		PortletConfig portletConfig = new MockPortletConfig(portletContext);
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor(portletContext, portletConfig);
		PortletContextAwareBean bean = new PortletContextAwareBean();
		assertNull(bean.getPortletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("PortletContext should have been set", bean.getPortletContext());
		assertEquals(portletContext, bean.getPortletContext());
	}

	public void testPortletContextAwareWithNullPortletContextAndNonNullPortletConfig() {
		PortletContext portletContext = new MockPortletContext();
		PortletConfig portletConfig = new MockPortletConfig(portletContext);
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor(null, portletConfig);
		PortletContextAwareBean bean = new PortletContextAwareBean();
		assertNull(bean.getPortletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("PortletContext should have been set", bean.getPortletContext());
		assertEquals(portletContext, bean.getPortletContext());
	}

	public void testPortletContextAwareWithNonNullPortletContextAndNullPortletConfig() {
		PortletContext portletContext = new MockPortletContext();
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor(portletContext, null);
		PortletContextAwareBean bean = new PortletContextAwareBean();
		assertNull(bean.getPortletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("PortletContext should have been set", bean.getPortletContext());
		assertEquals(portletContext, bean.getPortletContext());
	}

	public void testPortletContextAwareWithNullPortletContext() {
		PortletContext portletContext = null;
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor(portletContext);
		PortletContextAwareBean bean = new PortletContextAwareBean();
		assertNull(bean.getPortletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNull(bean.getPortletContext());
	}

	public void testPortletConfigAwareWithPortletContextOnly() {
		PortletContext portletContext = new MockPortletContext();
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor(portletContext);
		PortletConfigAwareBean bean = new PortletConfigAwareBean();
		assertNull(bean.getPortletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNull(bean.getPortletConfig());
	}

	public void testPortletConfigAwareWithPortletConfig() {
		PortletContext portletContext = new MockPortletContext();
		PortletConfig portletConfig = new MockPortletConfig(portletContext);
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor(portletConfig);
		PortletConfigAwareBean bean = new PortletConfigAwareBean();
		assertNull(bean.getPortletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("PortletConfig should have been set", bean.getPortletConfig());
		assertEquals(portletConfig, bean.getPortletConfig());
	}

	public void testPortletConfigAwareWithPortletContextAndPortletConfig() {
		PortletContext portletContext = new MockPortletContext();
		PortletConfig portletConfig = new MockPortletConfig(portletContext);
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor(portletContext, portletConfig);
		PortletConfigAwareBean bean = new PortletConfigAwareBean();
		assertNull(bean.getPortletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("PortletConfig should have been set", bean.getPortletConfig());
		assertEquals(portletConfig, bean.getPortletConfig());
	}

	public void testPortletConfigAwareWithNullPortletContextAndNonNullPortletConfig() {
		PortletContext portletContext = new MockPortletContext();
		PortletConfig portletConfig = new MockPortletConfig(portletContext);
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor(null, portletConfig);
		PortletConfigAwareBean bean = new PortletConfigAwareBean();
		assertNull(bean.getPortletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("PortletConfig should have been set", bean.getPortletConfig());
		assertEquals(portletConfig, bean.getPortletConfig());
	}

	public void testPortletConfigAwareWithNonNullPortletContextAndNullPortletConfig() {
		PortletContext portletContext = new MockPortletContext();
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor(portletContext, null);
		PortletConfigAwareBean bean = new PortletConfigAwareBean();
		assertNull(bean.getPortletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNull(bean.getPortletConfig());
	}

	public void testPortletConfigAwareWithNullPortletContext() {
		PortletContext portletContext = null;
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor(portletContext);
		PortletConfigAwareBean bean = new PortletConfigAwareBean();
		assertNull(bean.getPortletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNull(bean.getPortletConfig());
	}

}
