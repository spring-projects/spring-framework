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

package org.springframework.context.annotation.configuration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
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
	}

	@Test
	public void testScoped() {
		AnnotationConfigApplicationContext ctx =
				new AnnotationConfigApplicationContext(ScopedConfig.class, StandardPojo.class);
		assertFalse(ctx.getBeanFactory().containsSingleton("testBean1"));
		StandardPojo pojo = ctx.getBean(StandardPojo.class);
		assertThat(pojo.testBean.getName(), equalTo("interesting"));
	}

	@Test
	public void testScopedProxy() {
		AnnotationConfigApplicationContext ctx =
				new AnnotationConfigApplicationContext(ScopedProxyConfig.class, StandardPojo.class);
		assertTrue(ctx.getBeanFactory().containsSingleton("testBean1"));  // a shared scoped proxy
		StandardPojo pojo = ctx.getBean(StandardPojo.class);
		assertThat(pojo.testBean.getName(), equalTo("interesting"));
	}

	@Test
	public void testCustom() {
		AnnotationConfigApplicationContext ctx =
				new AnnotationConfigApplicationContext(CustomConfig.class, CustomPojo.class);
		assertFalse(ctx.getBeanFactory().containsSingleton("testBean1"));
		CustomPojo pojo = ctx.getBean(CustomPojo.class);
		assertThat(pojo.testBean.getName(), equalTo("interesting"));
	}


	@Configuration
	static class StandardConfig {

		@Bean @Qualifier("interesting") @Lazy
		public TestBean testBean1() {
			return new TestBean("interesting");
		}

		@Bean @Qualifier("boring")
		public TestBean testBean2() {
			return new TestBean("boring");
		}
	}

	@Configuration
	static class ScopedConfig {

		@Bean @Qualifier("interesting") @Scope("prototype")
		public TestBean testBean1() {
			return new TestBean("interesting");
		}

		@Bean @Qualifier("boring") @Scope("prototype")
		public TestBean testBean2() {
			return new TestBean("boring");
		}
	}

	@Configuration
	static class ScopedProxyConfig {

		@Bean @Qualifier("interesting") @Scope(value="prototype", proxyMode=ScopedProxyMode.TARGET_CLASS)
		public TestBean testBean1() {
			return new TestBean("interesting");
		}

		@Bean @Qualifier("boring") @Scope(value="prototype", proxyMode=ScopedProxyMode.TARGET_CLASS)
		public TestBean testBean2() {
			return new TestBean("boring");
		}
	}

	@Component @Lazy
	static class StandardPojo {

		@Autowired @Qualifier("interesting") TestBean testBean;
	}

	@Configuration
	static class CustomConfig {

		@InterestingBean
		public TestBean testBean1() {
			return new TestBean("interesting");
		}

		@Bean @Qualifier("boring")
		public TestBean testBean2() {
			return new TestBean("boring");
		}
	}

	@InterestingPojo
	static class CustomPojo {

		@InterestingNeed TestBean testBean;
	}

	@Bean @Lazy @Qualifier("interesting")
	@Retention(RetentionPolicy.RUNTIME)
	public @interface InterestingBean {
	}

	@Autowired @Qualifier("interesting")
	@Retention(RetentionPolicy.RUNTIME)
	public @interface InterestingNeed {
	}

	@Component @Lazy
	@Retention(RetentionPolicy.RUNTIME)
	public @interface InterestingPojo {
	}

}
