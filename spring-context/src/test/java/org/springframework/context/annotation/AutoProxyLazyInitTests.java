/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.context.annotation;

import javax.annotation.PreDestroy;

import org.junit.Test;

import org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.target.LazyInitTargetSourceCreator;
import org.springframework.aop.target.AbstractBeanFactoryBasedTargetSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Arrault Fabien
 */
public class AutoProxyLazyInitTests {

	@Test
	public void withStaticBeanMethod() {
		MyBeanImpl.initialized = false;

		ApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithStatic.class);
		MyBean bean = ctx.getBean("myBean", MyBean.class);

		assertFalse(MyBeanImpl.initialized);
		bean.doIt();
		assertTrue(MyBeanImpl.initialized);
	}

	@Test
	public void withStaticBeanMethodAndInterface() {
		MyBeanImpl.initialized = false;

		ApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithStaticAndInterface.class);
		MyBean bean = ctx.getBean("myBean", MyBean.class);

		assertFalse(MyBeanImpl.initialized);
		bean.doIt();
		assertTrue(MyBeanImpl.initialized);
	}

	@Test
	public void withNonStaticBeanMethod() {
		MyBeanImpl.initialized = false;

		ApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithNonStatic.class);
		MyBean bean = ctx.getBean("myBean", MyBean.class);

		assertFalse(MyBeanImpl.initialized);
		bean.doIt();
		assertTrue(MyBeanImpl.initialized);
	}

	@Test
	public void withNonStaticBeanMethodAndInterface() {
		MyBeanImpl.initialized = false;

		ApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithNonStaticAndInterface.class);
		MyBean bean = ctx.getBean("myBean", MyBean.class);

		assertFalse(MyBeanImpl.initialized);
		bean.doIt();
		assertTrue(MyBeanImpl.initialized);
	}


	public static interface MyBean {

		public String doIt();
	}


	public static class MyBeanImpl implements MyBean {

		public static boolean initialized = false;

		public MyBeanImpl() {
			initialized = true;
		}

		@Override
		public String doIt() {
			return "From implementation";
		}

		@PreDestroy
		public void destroy() {
		}
	}


	@Configuration
	public static class ConfigWithStatic {

		@Bean
		public BeanNameAutoProxyCreator lazyInitAutoProxyCreator() {
			BeanNameAutoProxyCreator autoProxyCreator = new BeanNameAutoProxyCreator();
			autoProxyCreator.setCustomTargetSourceCreators(lazyInitTargetSourceCreator());
			return autoProxyCreator;
		}

		@Bean
		public LazyInitTargetSourceCreator lazyInitTargetSourceCreator() {
			return new StrictLazyInitTargetSourceCreator();
		}

		@Bean
		@Lazy
		public static MyBean myBean() {
			return new MyBeanImpl();
		}
	}


	@Configuration
	public static class ConfigWithStaticAndInterface implements ApplicationListener<ApplicationContextEvent> {

		@Bean
		public BeanNameAutoProxyCreator lazyInitAutoProxyCreator() {
			BeanNameAutoProxyCreator autoProxyCreator = new BeanNameAutoProxyCreator();
			autoProxyCreator.setCustomTargetSourceCreators(lazyInitTargetSourceCreator());
			return autoProxyCreator;
		}

		@Bean
		public LazyInitTargetSourceCreator lazyInitTargetSourceCreator() {
			return new StrictLazyInitTargetSourceCreator();
		}

		@Bean
		@Lazy
		public static MyBean myBean() {
			return new MyBeanImpl();
		}

		@Override
		public void onApplicationEvent(ApplicationContextEvent event) {
		}
	}


	@Configuration
	public static class ConfigWithNonStatic {

		@Bean
		public BeanNameAutoProxyCreator lazyInitAutoProxyCreator() {
			BeanNameAutoProxyCreator autoProxyCreator = new BeanNameAutoProxyCreator();
			autoProxyCreator.setCustomTargetSourceCreators(lazyInitTargetSourceCreator());
			return autoProxyCreator;
		}

		@Bean
		public LazyInitTargetSourceCreator lazyInitTargetSourceCreator() {
			return new StrictLazyInitTargetSourceCreator();
		}

		@Bean
		@Lazy
		public MyBean myBean() {
			return new MyBeanImpl();
		}
	}


	@Configuration
	public static class ConfigWithNonStaticAndInterface implements ApplicationListener<ApplicationContextEvent> {

		@Bean
		public BeanNameAutoProxyCreator lazyInitAutoProxyCreator() {
			BeanNameAutoProxyCreator autoProxyCreator = new BeanNameAutoProxyCreator();
			autoProxyCreator.setCustomTargetSourceCreators(lazyInitTargetSourceCreator());
			return autoProxyCreator;
		}

		@Bean
		public LazyInitTargetSourceCreator lazyInitTargetSourceCreator() {
			return new StrictLazyInitTargetSourceCreator();
		}

		@Bean
		@Lazy
		public MyBean myBean() {
			return new MyBeanImpl();
		}

		@Override
		public void onApplicationEvent(ApplicationContextEvent event) {
		}
	}


	private static class StrictLazyInitTargetSourceCreator extends LazyInitTargetSourceCreator {

		@Override
		protected AbstractBeanFactoryBasedTargetSource createBeanFactoryBasedTargetSource(Class<?> beanClass, String beanName) {
			if ("myBean".equals(beanName)) {
				assertEquals(MyBean.class, beanClass);
			}
			return super.createBeanFactoryBasedTargetSource(beanClass, beanName);
		}
	}

}
