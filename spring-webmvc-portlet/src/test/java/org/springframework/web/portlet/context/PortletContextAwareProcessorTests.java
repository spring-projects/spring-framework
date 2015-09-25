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

package org.springframework.web.portlet.context;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;

import org.junit.Test;

import org.springframework.mock.web.portlet.MockPortletConfig;
import org.springframework.mock.web.portlet.MockPortletContext;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @author Sam Brannen
 */
public class PortletContextAwareProcessorTests {

	private final PortletContext portletContext = new MockPortletContext();

	private final PortletConfig portletConfig = new MockPortletConfig(portletContext);

	private final PortletContextAwareProcessor processor = new PortletContextAwareProcessor(portletContext);

	private final PortletContextAwareBean bean = new PortletContextAwareBean();


	@Test
	public void portletContextAwareWithPortletContext() {
		assertNull(bean.getPortletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("PortletContext should have been set", bean.getPortletContext());
		assertEquals(portletContext, bean.getPortletContext());
	}

	@Test
	public void portletContextAwareWithPortletConfig() {
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor(portletConfig);
		assertNull(bean.getPortletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("PortletContext should have been set", bean.getPortletContext());
		assertEquals(portletContext, bean.getPortletContext());
	}

	@Test
	public void portletContextAwareWithPortletContextAndPortletConfig() {
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor(portletContext, portletConfig);
		assertNull(bean.getPortletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("PortletContext should have been set", bean.getPortletContext());
		assertEquals(portletContext, bean.getPortletContext());
	}

	@Test
	public void portletContextAwareWithNullPortletContextAndNonNullPortletConfig() {
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor(null, portletConfig);
		assertNull(bean.getPortletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("PortletContext should have been set", bean.getPortletContext());
		assertEquals(portletContext, bean.getPortletContext());
	}

	@Test
	public void portletContextAwareWithNonNullPortletContextAndNullPortletConfig() {
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor(portletContext, null);
		assertNull(bean.getPortletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("PortletContext should have been set", bean.getPortletContext());
		assertEquals(portletContext, bean.getPortletContext());
	}

	@Test
	public void portletContextAwareWithNullPortletContext() {
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor((PortletContext) null);
		assertNull(bean.getPortletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNull(bean.getPortletContext());
	}

	@Test
	public void portletConfigAwareWithPortletContextOnly() {
		PortletConfigAwareBean bean = new PortletConfigAwareBean();
		assertNull(bean.getPortletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNull(bean.getPortletConfig());
	}

	@Test
	public void portletConfigAwareWithPortletConfig() {
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor(portletConfig);
		PortletConfigAwareBean bean = new PortletConfigAwareBean();
		assertNull(bean.getPortletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("PortletConfig should have been set", bean.getPortletConfig());
		assertEquals(portletConfig, bean.getPortletConfig());
	}

	@Test
	public void portletConfigAwareWithPortletContextAndPortletConfig() {
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor(portletContext, portletConfig);
		PortletConfigAwareBean bean = new PortletConfigAwareBean();
		assertNull(bean.getPortletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("PortletConfig should have been set", bean.getPortletConfig());
		assertEquals(portletConfig, bean.getPortletConfig());
	}

	@Test
	public void portletConfigAwareWithNullPortletContextAndNonNullPortletConfig() {
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor(null, portletConfig);
		PortletConfigAwareBean bean = new PortletConfigAwareBean();
		assertNull(bean.getPortletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("PortletConfig should have been set", bean.getPortletConfig());
		assertEquals(portletConfig, bean.getPortletConfig());
	}

	@Test
	public void portletConfigAwareWithNonNullPortletContextAndNullPortletConfig() {
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor(portletContext, null);
		PortletConfigAwareBean bean = new PortletConfigAwareBean();
		assertNull(bean.getPortletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNull(bean.getPortletConfig());
	}

	@Test
	public void portletConfigAwareWithNullPortletContext() {
		PortletContextAwareProcessor processor = new PortletContextAwareProcessor((PortletContext) null);
		PortletConfigAwareBean bean = new PortletConfigAwareBean();
		assertNull(bean.getPortletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNull(bean.getPortletConfig());
	}

}
