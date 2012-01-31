/*
 * Copyright 2002-2009 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;

import test.beans.TestBean;


/**
 * System tests covering use of AspectJ {@link Aspect}s in conjunction with {@link Configuration} classes.
 * {@link Bean} methods may return aspects, or Configuration classes may themselves be annotated with Aspect.
 * In the latter case, advice methods are declared inline within the Configuration class.  This makes for a
 * particularly convenient syntax requiring no extra artifact for the aspect.
 * 
 * <p>Currently it is assumed that the user is bootstrapping Configuration class processing via XML (using
 * annotation-config or component-scan), and thus will also use {@code <aop:aspectj-autoproxy/>} to enable
 * processing of the Aspect annotation.
 *
 * @author Chris Beams
 */
public class ConfigurationClassAspectIntegrationTests {
	private void assertAdviceWasApplied(Class<?> configClass) {
		GenericApplicationContext ctx = new GenericApplicationContext(
					new XmlBeanFactory(new ClassPathResource("aspectj-autoproxy-config.xml", ConfigurationClassAspectIntegrationTests.class)));
		ctx.addBeanFactoryPostProcessor(new ConfigurationClassPostProcessor());
		ctx.registerBeanDefinition("config", new RootBeanDefinition(configClass));
		ctx.refresh();

		TestBean testBean = ctx.getBean("testBean", TestBean.class);
		assertThat(testBean.getName(), equalTo("name"));
		testBean.absquatulate();
		assertThat(testBean.getName(), equalTo("advisedName"));
	}

	@Test
	public void aspectAnnotatedConfiguration() {
		assertAdviceWasApplied(AspectConfig.class);
	}

	@Test
	public void configurationIncludesAspect() {
		assertAdviceWasApplied(ConfigurationWithAspect.class);
	}


	@Aspect
	@Configuration
	static class AspectConfig {
		@Bean
		public TestBean testBean() {
			return new TestBean("name");
		}

		@Before("execution(* test.beans.TestBean.absquatulate(..)) && target(testBean)")
		public void touchBean(TestBean testBean) {
			testBean.setName("advisedName");
		}
	}

	@Configuration
	static class ConfigurationWithAspect {
		@Bean
		public TestBean testBean() {
			return new TestBean("name");
		}

		@Bean
		public NameChangingAspect nameChangingAspect() {
			return new NameChangingAspect();
		}
	}

	@Aspect
	static class NameChangingAspect {
		@Before("execution(* test.beans.TestBean.absquatulate(..)) && target(testBean)")
		public void touchBean(TestBean testBean) {
			testBean.setName("advisedName");
		}
	}
}
