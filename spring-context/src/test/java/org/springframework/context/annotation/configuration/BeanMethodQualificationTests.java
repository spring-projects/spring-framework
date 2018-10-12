/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context.annotation.configuration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.tests.sample.beans.NestedTestBean;
import org.springframework.tests.sample.beans.TestBean;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Tests proving that @Qualifier annotations work when used
 * with @Configuration classes on @Bean methods.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public class BeanMethodQualificationTests {

	@Test
	public void testStandard() {
		AnnotationConfigApplicationContext ctx =
				new AnnotationConfigApplicationContext(StandardConfig.class, StandardPojo.class);
		assertFalse(ctx.getBeanFactory().containsSingleton("testBean1"));
		StandardPojo pojo = ctx.getBean(StandardPojo.class);
		assertThat(pojo.testBean.getName(), equalTo("interesting"));
		assertThat(pojo.testBean2.getName(), equalTo("boring"));
	}

	@Test
	public void testScoped() {
		AnnotationConfigApplicationContext ctx =
				new AnnotationConfigApplicationContext(ScopedConfig.class, StandardPojo.class);
		assertFalse(ctx.getBeanFactory().containsSingleton("testBean1"));
		StandardPojo pojo = ctx.getBean(StandardPojo.class);
		assertThat(pojo.testBean.getName(), equalTo("interesting"));
		assertThat(pojo.testBean2.getName(), equalTo("boring"));
	}

	@Test
	public void testScopedProxy() {
		AnnotationConfigApplicationContext ctx =
				new AnnotationConfigApplicationContext(ScopedProxyConfig.class, StandardPojo.class);
		assertTrue(ctx.getBeanFactory().containsSingleton("testBean1"));  // a shared scoped proxy
		StandardPojo pojo = ctx.getBean(StandardPojo.class);
		assertThat(pojo.testBean.getName(), equalTo("interesting"));
		assertThat(pojo.testBean2.getName(), equalTo("boring"));
	}

	@Test
	public void testCustomWithLazyResolution() {
		AnnotationConfigApplicationContext ctx =
				new AnnotationConfigApplicationContext(CustomConfig.class, CustomPojo.class);
		assertFalse(ctx.getBeanFactory().containsSingleton("testBean1"));
		assertFalse(ctx.getBeanFactory().containsSingleton("testBean2"));
		assertTrue(BeanFactoryAnnotationUtils.isQualifierMatch(value -> value.equals("boring"),
				"testBean2", ctx.getDefaultListableBeanFactory()));
		CustomPojo pojo = ctx.getBean(CustomPojo.class);
		assertThat(pojo.testBean.getName(), equalTo("interesting"));
		TestBean testBean2 = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
				ctx.getDefaultListableBeanFactory(), TestBean.class, "boring");
		assertThat(testBean2.getName(), equalTo("boring"));
	}

	@Test
	public void testCustomWithEarlyResolution() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(CustomConfig.class, CustomPojo.class);
		ctx.refresh();
		assertFalse(ctx.getBeanFactory().containsSingleton("testBean1"));
		assertFalse(ctx.getBeanFactory().containsSingleton("testBean2"));
		ctx.getBean("testBean2");
		assertTrue(BeanFactoryAnnotationUtils.isQualifierMatch(value -> value.equals("boring"),
				"testBean2", ctx.getDefaultListableBeanFactory()));
		CustomPojo pojo = ctx.getBean(CustomPojo.class);
		assertThat(pojo.testBean.getName(), equalTo("interesting"));
	}

	@Test
	public void testCustomWithAsm() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.registerBeanDefinition("customConfig", new RootBeanDefinition(CustomConfig.class.getName()));
		RootBeanDefinition customPojo = new RootBeanDefinition(CustomPojo.class.getName());
		customPojo.setLazyInit(true);
		ctx.registerBeanDefinition("customPojo", customPojo);
		ctx.refresh();
		assertFalse(ctx.getBeanFactory().containsSingleton("testBean1"));
		assertFalse(ctx.getBeanFactory().containsSingleton("testBean2"));
		CustomPojo pojo = ctx.getBean(CustomPojo.class);
		assertThat(pojo.testBean.getName(), equalTo("interesting"));
	}

	@Test
	public void testCustomWithAttributeOverride() {
		AnnotationConfigApplicationContext ctx =
				new AnnotationConfigApplicationContext(CustomConfigWithAttributeOverride.class, CustomPojo.class);
		assertFalse(ctx.getBeanFactory().containsSingleton("testBeanX"));
		CustomPojo pojo = ctx.getBean(CustomPojo.class);
		assertThat(pojo.testBean.getName(), equalTo("interesting"));
	}


	@Configuration
	static class StandardConfig {

		@Bean @Qualifier("interesting") @Lazy
		public TestBean testBean1() {
			return new TestBean("interesting");
		}

		@Bean @Boring
		public TestBean testBean2(@Lazy TestBean testBean1) {
			TestBean tb = new TestBean("boring");
			tb.setSpouse(testBean1);
			return tb;
		}
	}

	@Configuration
	static class ScopedConfig {

		@Bean @Qualifier("interesting") @Scope("prototype")
		public TestBean testBean1() {
			return new TestBean("interesting");
		}

		@Bean @Boring @Scope("prototype")
		public TestBean testBean2(TestBean testBean1) {
			TestBean tb = new TestBean("boring");
			tb.setSpouse(testBean1);
			return tb;
		}
	}

	@Configuration
	static class ScopedProxyConfig {

		@Bean @Qualifier("interesting") @Scope(value="prototype", proxyMode=ScopedProxyMode.TARGET_CLASS)
		public TestBean testBean1() {
			return new TestBean("interesting");
		}

		@Bean @Boring @Scope(value="prototype", proxyMode=ScopedProxyMode.TARGET_CLASS)
		public TestBean testBean2(TestBean testBean1) {
			TestBean tb = new TestBean("boring");
			tb.setSpouse(testBean1);
			return tb;
		}
	}

	@Component @Lazy
	static class StandardPojo {

		@Autowired @Qualifier("interesting") TestBean testBean;

		@Autowired @Boring TestBean testBean2;
	}

	@Qualifier
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Boring {
	}

	@Configuration
	static class CustomConfig {

		@InterestingBean
		public TestBean testBean1() {
			return new TestBean("interesting");
		}

		@Bean @Qualifier("boring") @Lazy
		public TestBean testBean2(@Lazy TestBean testBean1) {
			TestBean tb = new TestBean("boring");
			tb.setSpouse(testBean1);
			return tb;
		}
	}

	@Configuration
	static class CustomConfigWithAttributeOverride {

		@InterestingBeanWithName(name="testBeanX")
		public TestBean testBean1() {
			return new TestBean("interesting");
		}

		@Bean @Qualifier("boring")
		public TestBean testBean2(@Lazy TestBean testBean1) {
			TestBean tb = new TestBean("boring");
			tb.setSpouse(testBean1);
			return tb;
		}
	}

	@InterestingPojo
	static class CustomPojo {

		@InterestingNeed TestBean testBean;

		@InterestingNeedWithRequiredOverride(required=false) NestedTestBean nestedTestBean;
	}

	@Bean @Lazy @Qualifier("interesting")
	@Retention(RetentionPolicy.RUNTIME)
	public @interface InterestingBean {
	}

	@Bean @Lazy @Qualifier("interesting")
	@Retention(RetentionPolicy.RUNTIME)
	public @interface InterestingBeanWithName {

		String name();
	}

	@Autowired @Qualifier("interesting")
	@Retention(RetentionPolicy.RUNTIME)
	public @interface InterestingNeed {
	}

	@Autowired @Qualifier("interesting")
	@Retention(RetentionPolicy.RUNTIME)
	public @interface InterestingNeedWithRequiredOverride {

		boolean required();
	}

	@Component @Lazy
	@Retention(RetentionPolicy.RUNTIME)
	public @interface InterestingPojo {
	}

}
