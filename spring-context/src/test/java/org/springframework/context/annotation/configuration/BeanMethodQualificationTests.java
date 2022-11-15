/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.context.annotation.configuration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.NestedTestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests proving that @Qualifier annotations work when used
 * with @Configuration classes on @Bean methods.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
class BeanMethodQualificationTests {

	@Test
	void standard() {
		AnnotationConfigApplicationContext ctx =
				new AnnotationConfigApplicationContext(StandardConfig.class, StandardPojo.class);
		assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isFalse();
		StandardPojo pojo = ctx.getBean(StandardPojo.class);
		assertThat(pojo.testBean.getName()).isEqualTo("interesting");
		assertThat(pojo.testBean2.getName()).isEqualTo("boring");
		ctx.close();
	}

	@Test
	void scoped() {
		AnnotationConfigApplicationContext ctx =
				new AnnotationConfigApplicationContext(ScopedConfig.class, StandardPojo.class);
		assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isFalse();
		StandardPojo pojo = ctx.getBean(StandardPojo.class);
		assertThat(pojo.testBean.getName()).isEqualTo("interesting");
		assertThat(pojo.testBean2.getName()).isEqualTo("boring");
		ctx.close();
	}

	@Test
	void scopedProxy() {
		AnnotationConfigApplicationContext ctx =
				new AnnotationConfigApplicationContext(ScopedProxyConfig.class, StandardPojo.class);
		assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isTrue();  // a shared scoped proxy
		StandardPojo pojo = ctx.getBean(StandardPojo.class);
		assertThat(pojo.testBean.getName()).isEqualTo("interesting");
		assertThat(pojo.testBean2.getName()).isEqualTo("boring");
		ctx.close();
	}

	@Test
	void customWithLazyResolution() {
		AnnotationConfigApplicationContext ctx =
				new AnnotationConfigApplicationContext(CustomConfig.class, CustomPojo.class);
		assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isFalse();
		assertThat(ctx.getBeanFactory().containsSingleton("testBean2")).isFalse();
		assertThat(BeanFactoryAnnotationUtils.isQualifierMatch(value -> value.equals("boring"),
		"testBean2", ctx.getDefaultListableBeanFactory())).isTrue();
		CustomPojo pojo = ctx.getBean(CustomPojo.class);
		assertThat(pojo.testBean.getName()).isEqualTo("interesting");
		TestBean testBean2 = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
				ctx.getDefaultListableBeanFactory(), TestBean.class, "boring");
		assertThat(testBean2.getName()).isEqualTo("boring");
		ctx.close();
	}

	@Test
	void customWithEarlyResolution() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(CustomConfig.class, CustomPojo.class);
		ctx.refresh();
		assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isFalse();
		assertThat(ctx.getBeanFactory().containsSingleton("testBean2")).isFalse();
		ctx.getBean("testBean2");
		assertThat(BeanFactoryAnnotationUtils.isQualifierMatch(value -> value.equals("boring"),
		"testBean2", ctx.getDefaultListableBeanFactory())).isTrue();
		CustomPojo pojo = ctx.getBean(CustomPojo.class);
		assertThat(pojo.testBean.getName()).isEqualTo("interesting");
		ctx.close();
	}

	@Test
	void customWithAsm() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.registerBeanDefinition("customConfig", new RootBeanDefinition(CustomConfig.class.getName()));
		RootBeanDefinition customPojo = new RootBeanDefinition(CustomPojo.class.getName());
		customPojo.setLazyInit(true);
		ctx.registerBeanDefinition("customPojo", customPojo);
		ctx.refresh();
		assertThat(ctx.getBeanFactory().containsSingleton("testBean1")).isFalse();
		assertThat(ctx.getBeanFactory().containsSingleton("testBean2")).isFalse();
		CustomPojo pojo = ctx.getBean(CustomPojo.class);
		assertThat(pojo.testBean.getName()).isEqualTo("interesting");
		ctx.close();
	}

	@Test
	void customWithAttributeOverride() {
		AnnotationConfigApplicationContext ctx =
				new AnnotationConfigApplicationContext(CustomConfigWithAttributeOverride.class, CustomPojo.class);
		assertThat(ctx.getBeanFactory().containsSingleton("testBeanX")).isFalse();
		CustomPojo pojo = ctx.getBean(CustomPojo.class);
		assertThat(pojo.testBean.getName()).isEqualTo("interesting");
		ctx.close();
	}

	@Test
	void beanNamesForAnnotation() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(StandardConfig.class);
		assertThat(ctx.getBeanNamesForAnnotation(Configuration.class)).isEqualTo(new String[] {"beanMethodQualificationTests.StandardConfig"});
		assertThat(ctx.getBeanNamesForAnnotation(Scope.class)).isEqualTo(new String[] {});
		assertThat(ctx.getBeanNamesForAnnotation(Lazy.class)).isEqualTo(new String[] {"testBean1"});
		assertThat(ctx.getBeanNamesForAnnotation(Boring.class)).isEqualTo(new String[] {"beanMethodQualificationTests.StandardConfig", "testBean2"});
		ctx.close();
	}


	@Configuration
	@Boring
	static class StandardConfig {

		@Bean @Qualifier("interesting") @Lazy
		public static TestBean testBean1() {
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
	@Boring
	static class ScopedConfig {

		@Bean @Qualifier("interesting") @Scope("prototype")
		public static TestBean testBean1() {
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
	@Boring
	static class ScopedProxyConfig {

		@Bean @Qualifier("interesting") @Scope(value="prototype", proxyMode=ScopedProxyMode.TARGET_CLASS)
		public static TestBean testBean1() {
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
	@interface InterestingBean {
	}

	@Bean @Lazy @Qualifier("interesting")
	@Retention(RetentionPolicy.RUNTIME)
	@interface InterestingBeanWithName {

		@AliasFor(annotation = Bean.class)
		String name();
	}

	@Autowired @Qualifier("interesting")
	@Retention(RetentionPolicy.RUNTIME)
	@interface InterestingNeed {
	}

	@Autowired @Qualifier("interesting")
	@Retention(RetentionPolicy.RUNTIME)
	@interface InterestingNeedWithRequiredOverride {

		@AliasFor(annotation = Autowired.class)
		boolean required();
	}

	@Component @Lazy
	@Retention(RetentionPolicy.RUNTIME)
	@interface InterestingPojo {
	}

}
