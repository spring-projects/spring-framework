/*
 * Copyright 2002-2011 the original author or authors.
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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.context.annotation.configuration.StubSpecification;
import org.springframework.context.config.FeatureSpecification;

import test.beans.ITestBean;
import test.beans.TestBean;

/**
 * Tests proving that @Feature methods may reference the product of @Bean methods.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class FeatureMethodBeanReferenceTests {

	@Test
	public void test() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(FeatureConfig.class, Config.class);
		ctx.refresh();

		TestBean registeredBean = ctx.getBean("testBean", TestBean.class);
		TestBean proxiedBean = ctx.getBean(FeatureConfig.class).testBean;

		assertThat(registeredBean, not(instanceOf(EarlyBeanReferenceProxy.class)));
		assertThat(proxiedBean, notNullValue());
		assertThat(proxiedBean, instanceOf(EarlyBeanReferenceProxy.class));
		assertThat(proxiedBean.getSpouse(), is(registeredBean.getSpouse()));
	}


	@FeatureConfiguration
	static class FeatureConfig {
		TestBean testBean;

		@Feature
		public FeatureSpecification f(TestBean testBean) {
			this.testBean = testBean;
			return new StubSpecification();
		}
	}


	@Configuration
	static class Config {
		@Bean
		public ITestBean testBean() {
			return new TestBean(new TestBean("mySpouse"));
		}
	}

}
