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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.configuration.StubSpecification;
import org.springframework.context.config.FeatureSpecification;

import test.beans.ITestBean;
import test.beans.TestBean;

/**
 * Tests proving that @Feature methods may reference beans using @Qualifier
 * as a parameter annotation.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class FeatureMethodQualifiedBeanReferenceTests {

	@Test
	public void test() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(Features.class, TestBeans.class);
		ctx.refresh();
	}

	@FeatureConfiguration
	static class Features {

		@Feature
		public FeatureSpecification f(@Qualifier("testBean1") ITestBean testBean) {
			assertThat(testBean.getName(), equalTo("one"));
			return new StubSpecification();
		}

	}

	@Configuration
	static class TestBeans {
		@Bean
		public ITestBean testBean1() {
			return new TestBean("one");
		}

		@Bean
		public ITestBean testBean2() {
			return new TestBean("two");
		}
	}

}
