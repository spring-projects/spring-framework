/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.Locale;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;

import org.springframework.beans.BeansException;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.NoSuchMessageException;
import org.springframework.mock.web.portlet.MockPortletConfig;
import org.springframework.mock.web.portlet.MockPortletContext;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Chris Beams
 */
public class XmlPortletApplicationContextTests extends AbstractXmlWebApplicationContextTests {

	private ConfigurablePortletApplicationContext root;

	@Override
	protected ConfigurableApplicationContext createContext() throws Exception {
		root = new XmlPortletApplicationContext();
		PortletContext portletContext = new MockPortletContext();
		PortletConfig portletConfig = new MockPortletConfig(portletContext);
		root.setPortletConfig(portletConfig);
		root.setConfigLocations(new String[] {"/org/springframework/web/portlet/context/WEB-INF/applicationContext.xml"});
		root.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor() {
			@Override
			public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
				beanFactory.addBeanPostProcessor(new BeanPostProcessor() {
					@Override
					public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
						if(bean instanceof TestBean) {
							((TestBean) bean).getFriends().add("myFriend");
						}
						return bean;
					}

					@Override
					public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
						return bean;
					}
				});
			}
		});
		root.refresh();
		XmlPortletApplicationContext pac = new XmlPortletApplicationContext();
		pac.setParent(root);
		pac.setPortletConfig(portletConfig);
		pac.setNamespace("test-portlet");
		pac.setConfigLocations(new String[] {"/org/springframework/web/portlet/context/WEB-INF/test-portlet.xml"});
		pac.refresh();
		return pac;
	}

	/**
	 * Overridden in order to use MockPortletConfig
	 * @see org.springframework.web.context.XmlWebApplicationContextTests#testWithoutMessageSource()
	 */
	public void testWithoutMessageSource() throws Exception {
		MockPortletContext portletContext = new MockPortletContext("");
		MockPortletConfig portletConfig = new MockPortletConfig(portletContext);
		XmlPortletApplicationContext pac = new XmlPortletApplicationContext();
		pac.setParent(root);
		pac.setPortletConfig(portletConfig);
		pac.setNamespace("testNamespace");
		pac.setConfigLocations(new String[] {"/org/springframework/web/portlet/context/WEB-INF/test-portlet.xml"});
		pac.refresh();
		try {
			pac.getMessage("someMessage", null, Locale.getDefault());
			fail("Should have thrown NoSuchMessageException");
		}
		catch (NoSuchMessageException ex) {
			// expected;
		}
		String msg = pac.getMessage("someMessage", null, "default", Locale.getDefault());
		assertTrue("Default message returned", "default".equals(msg));
	}

	/**
	 * Overridden in order to access the root ApplicationContext
	 * @see org.springframework.web.context.XmlWebApplicationContextTests#testContextNesting()
	 */
	@Override
	public void testContextNesting() {
		TestBean father = (TestBean) this.applicationContext.getBean("father");
		assertTrue("Bean from root context", father != null);
		assertTrue("Custom BeanPostProcessor applied", father.getFriends().contains("myFriend"));

		TestBean rod = (TestBean) this.applicationContext.getBean("rod");
		assertTrue("Bean from child context", "Rod".equals(rod.getName()));
		assertTrue("Bean has external reference", rod.getSpouse() == father);
		assertTrue("Custom BeanPostProcessor not applied", !rod.getFriends().contains("myFriend"));

		rod = (TestBean) this.root.getBean("rod");
		assertTrue("Bean from root context", "Roderick".equals(rod.getName()));
		assertTrue("Custom BeanPostProcessor applied", rod.getFriends().contains("myFriend"));
	}

	@Override
	public void testCount() {
		assertTrue("should have 16 beans, not "+ this.applicationContext.getBeanDefinitionCount(),
				this.applicationContext.getBeanDefinitionCount() == 16);
	}

	public void testPortletContextAwareBean() {
		PortletContextAwareBean bean = (PortletContextAwareBean)this.applicationContext.getBean("portletContextAwareBean");
		assertNotNull(bean.getPortletContext());
	}

	public void testPortletConfigAwareBean() {
		PortletConfigAwareBean bean = (PortletConfigAwareBean)this.applicationContext.getBean("portletConfigAwareBean");
		assertNotNull(bean.getPortletConfig());
	}

}
