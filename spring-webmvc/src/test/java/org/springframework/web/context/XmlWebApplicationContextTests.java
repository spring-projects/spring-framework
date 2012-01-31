/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.web.context;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.util.Locale;

import javax.servlet.ServletException;

import org.springframework.beans.BeansException;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.TestListener;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class XmlWebApplicationContextTests extends AbstractApplicationContextTests {

	private ConfigurableWebApplicationContext root;

	protected ConfigurableApplicationContext createContext() throws Exception {
		InitAndIB.constructed = false;
		root = new XmlWebApplicationContext();
		MockServletContext sc = new MockServletContext("");
		root.setServletContext(sc);
		root.setConfigLocations(new String[] {"/org/springframework/web/context/WEB-INF/applicationContext.xml"});
		root.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor() {
			public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
				beanFactory.addBeanPostProcessor(new BeanPostProcessor() {
					public Object postProcessBeforeInitialization(Object bean, String name) throws BeansException {
						if (bean instanceof TestBean) {
							((TestBean) bean).getFriends().add("myFriend");
						}
						return bean;
					}
					public Object postProcessAfterInitialization(Object bean, String name) throws BeansException {
						return bean;
					}
				});
			}
		});
		root.refresh();
		XmlWebApplicationContext wac = new XmlWebApplicationContext();
		wac.setParent(root);
		wac.setServletContext(sc);
		wac.setNamespace("test-servlet");
		wac.setConfigLocations(new String[] {"/org/springframework/web/context/WEB-INF/test-servlet.xml"});
		wac.refresh();
		return wac;
	}

	public void testEnvironmentInheritance() {
		assertThat(this.applicationContext.getEnvironment(), sameInstance(this.root.getEnvironment()));
	}

	/**
	 * Overridden as we can't trust superclass method
	 * @see org.springframework.context.AbstractApplicationContextTests#testEvents()
	 */
	public void testEvents() throws Exception {
		TestListener listener = (TestListener) this.applicationContext.getBean("testListener");
		listener.zeroCounter();
		TestListener parentListener = (TestListener) this.applicationContext.getParent().getBean("parentListener");
		parentListener.zeroCounter();
		
		parentListener.zeroCounter();
		assertTrue("0 events before publication", listener.getEventCount() == 0);
		assertTrue("0 parent events before publication", parentListener.getEventCount() == 0);
		this.applicationContext.publishEvent(new MyEvent(this));
		assertTrue("1 events after publication, not " + listener.getEventCount(), listener.getEventCount() == 1);
		assertTrue("1 parent events after publication", parentListener.getEventCount() == 1);
	}

	public void testCount() {
		assertTrue("should have 14 beans, not "+ this.applicationContext.getBeanDefinitionCount(),
			this.applicationContext.getBeanDefinitionCount() == 14);
	}

	public void testWithoutMessageSource() throws Exception {
		MockServletContext sc = new MockServletContext("");
		XmlWebApplicationContext wac = new XmlWebApplicationContext();
		wac.setParent(root);
		wac.setServletContext(sc);
		wac.setNamespace("testNamespace");
		wac.setConfigLocations(new String[] {"/org/springframework/web/context/WEB-INF/test-servlet.xml"});
		wac.refresh();
		try {
			wac.getMessage("someMessage", null, Locale.getDefault());
			fail("Should have thrown NoSuchMessageException");
		}
		catch (NoSuchMessageException ex) {
			// expected;
		}
		String msg = wac.getMessage("someMessage", null, "default", Locale.getDefault());
		assertTrue("Default message returned", "default".equals(msg));
	}

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

	public void testInitializingBeanAndInitMethod() throws Exception {
		assertFalse(InitAndIB.constructed);
		InitAndIB iib = (InitAndIB) this.applicationContext.getBean("init-and-ib");
		assertTrue(InitAndIB.constructed);
		assertTrue(iib.afterPropertiesSetInvoked && iib.initMethodInvoked);
		assertTrue(!iib.destroyed && !iib.customDestroyed);
		this.applicationContext.close();
		assertTrue(!iib.destroyed && !iib.customDestroyed);
		ConfigurableApplicationContext parent = (ConfigurableApplicationContext) this.applicationContext.getParent();
		parent.close();
		assertTrue(iib.destroyed && iib.customDestroyed);
		parent.close();
		assertTrue(iib.destroyed && iib.customDestroyed);
	}


	public static class InitAndIB implements InitializingBean, DisposableBean {

		public static boolean constructed;

		public boolean afterPropertiesSetInvoked, initMethodInvoked, destroyed, customDestroyed;

		public InitAndIB() {
			constructed = true;
		}

		public void afterPropertiesSet() {
			if (this.initMethodInvoked)
				fail();
			this.afterPropertiesSetInvoked = true;
		}

		/** Init method */
		public void customInit() throws ServletException {
			if (!this.afterPropertiesSetInvoked)
				fail();
			this.initMethodInvoked = true;
		}

		public void destroy() {
			if (this.customDestroyed)
				fail();
			if (this.destroyed) {
				throw new IllegalStateException("Already destroyed");
			}
			this.destroyed = true;
		}

		public void customDestroy() {
			if (!this.destroyed)
				fail();
			if (this.customDestroyed) {
				throw new IllegalStateException("Already customDestroyed");
			}
			this.customDestroyed = true;
		}
	}

}
