/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.context;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.testfixture.AbstractApplicationContextTests;
import org.springframework.context.testfixture.beans.TestApplicationListener;
import org.springframework.util.Assert;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
class XmlWebApplicationContextTests extends AbstractApplicationContextTests {

	private ConfigurableWebApplicationContext root;

	@Override
	protected ConfigurableApplicationContext createContext() {
		InitAndIB.constructed = false;
		root = new XmlWebApplicationContext();
		root.getEnvironment().addActiveProfile("rootProfile1");
		MockServletContext sc = new MockServletContext("");
		root.setServletContext(sc);
		root.setConfigLocations("/org/springframework/web/context/WEB-INF/applicationContext.xml");
		root.addBeanFactoryPostProcessor(beanFactory -> beanFactory.addBeanPostProcessor(new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String name) throws BeansException {
				if (bean instanceof TestBean testBean) {
					testBean.getFriends().add("myFriend");
				}
				return bean;
			}
		}));
		root.refresh();
		XmlWebApplicationContext wac = new XmlWebApplicationContext();
		wac.getEnvironment().addActiveProfile("wacProfile1");
		wac.setParent(root);
		wac.setServletContext(sc);
		wac.setNamespace("test-servlet");
		wac.setConfigLocations("/org/springframework/web/context/WEB-INF/test-servlet.xml");
		wac.refresh();
		return wac;
	}

	@Test
	@SuppressWarnings("deprecation")
	void environmentMerge() {
		assertThat(this.root.getEnvironment().acceptsProfiles("rootProfile1")).isTrue();
		assertThat(this.root.getEnvironment().acceptsProfiles("wacProfile1")).isFalse();
		assertThat(this.applicationContext.getEnvironment().acceptsProfiles("rootProfile1")).isTrue();
		assertThat(this.applicationContext.getEnvironment().acceptsProfiles("wacProfile1")).isTrue();
	}

	/**
	 * Overridden as we can't trust superclass method.
	 */
	@Override
	protected void doTestEvents(TestApplicationListener listener, TestApplicationListener parentListener,
			MyEvent event) {
		TestApplicationListener listenerBean = (TestApplicationListener) this.applicationContext.getBean("testListener");
		TestApplicationListener parentListenerBean = (TestApplicationListener) this.applicationContext.getParent().getBean("parentListener");
		super.doTestEvents(listenerBean, parentListenerBean, event);
	}

	@Test
	@Override
	protected void count() {
		assertThat(this.applicationContext.getBeanDefinitionCount()).as("should have 14 beans").isEqualTo(14);
	}

	@Test
	void withoutMessageSource() {
		MockServletContext sc = new MockServletContext("");
		XmlWebApplicationContext wac = new XmlWebApplicationContext();
		wac.setParent(root);
		wac.setServletContext(sc);
		wac.setNamespace("testNamespace");
		wac.setConfigLocations("/org/springframework/web/context/WEB-INF/test-servlet.xml");
		wac.refresh();
		assertThatExceptionOfType(NoSuchMessageException.class).isThrownBy(() ->
				wac.getMessage("someMessage", null, Locale.getDefault()));
		String msg = wac.getMessage("someMessage", null, "default", Locale.getDefault());
		assertThat(msg).as("Default message returned").isEqualTo("default");
	}

	@Test
	void contextNesting() {
		TestBean father = (TestBean) this.applicationContext.getBean("father");
		assertThat(father).as("Bean from root context").isNotNull();
		assertThat(father.getFriends().contains("myFriend")).as("Custom BeanPostProcessor applied").isTrue();

		TestBean rod = (TestBean) this.applicationContext.getBean("rod");
		assertThat(rod.getName()).as("Bean from child context").isEqualTo("Rod");
		assertThat(rod.getSpouse()).as("Bean has external reference").isSameAs(father);
		assertThat(rod.getFriends().contains("myFriend")).as("Custom BeanPostProcessor not applied").isFalse();

		rod = (TestBean) this.root.getBean("rod");
		assertThat(rod.getName()).as("Bean from root context").isEqualTo("Roderick");
		assertThat(rod.getFriends().contains("myFriend")).as("Custom BeanPostProcessor applied").isTrue();
	}

	@Test
	void initializingBeanAndInitMethod() {
		assertThat(InitAndIB.constructed).isFalse();
		InitAndIB iib = (InitAndIB) this.applicationContext.getBean("init-and-ib");
		assertThat(InitAndIB.constructed).isTrue();
		assertThat(iib.afterPropertiesSetInvoked && iib.initMethodInvoked).isTrue();
		assertThat(!iib.destroyed && !iib.customDestroyed).isTrue();
		this.applicationContext.close();
		assertThat(!iib.destroyed && !iib.customDestroyed).isTrue();
		ConfigurableApplicationContext parent = (ConfigurableApplicationContext) this.applicationContext.getParent();
		parent.close();
		assertThat(iib.destroyed && iib.customDestroyed).isTrue();
		parent.close();
		assertThat(iib.destroyed && iib.customDestroyed).isTrue();
	}


	public static class InitAndIB implements InitializingBean, DisposableBean {

		public static boolean constructed;

		public boolean afterPropertiesSetInvoked, initMethodInvoked, destroyed, customDestroyed;

		public InitAndIB() {
			constructed = true;
		}

		@Override
		public void afterPropertiesSet() {
			assertThat(this.initMethodInvoked).isFalse();
			this.afterPropertiesSetInvoked = true;
		}

		/** Init method */
		public void customInit() {
			assertThat(this.afterPropertiesSetInvoked).isTrue();
			this.initMethodInvoked = true;
		}

		@Override
		public void destroy() {
			assertThat(this.customDestroyed).isFalse();
			Assert.state(!this.destroyed, "Already destroyed");
			this.destroyed = true;
		}

		public void customDestroy() {
			assertThat(this.destroyed).isTrue();
			Assert.state(!this.customDestroyed, "Already customDestroyed");
			this.customDestroyed = true;
		}
	}

}
