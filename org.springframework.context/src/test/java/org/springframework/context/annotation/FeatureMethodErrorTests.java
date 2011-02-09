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
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.context.annotation.configuration.StubSpecification;
import org.springframework.context.config.FeatureSpecification;

import test.beans.TestBean;

/**
 * Tests proving that @Feature methods may reference the product of @Bean methods.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class FeatureMethodErrorTests {

	@Test
	public void incorrectReturnType() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(FeatureConfig.class);
		try {
			ctx.refresh();
			fail("expected exception");
		} catch (FeatureMethodExecutionException ex) {
			assertThat(ex.getCause().getMessage(),
					equalTo("Return type for @Feature method FeatureConfig.f() must be " +
							"assignable to FeatureSpecification"));
		}
	}

	@Test
	public void voidReturnType() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(VoidFeatureConfig.class);
		try {
			ctx.refresh();
			fail("expected exception");
		} catch (FeatureMethodExecutionException ex) {
			assertThat(ex.getCause().getMessage(),
					equalTo("Return type for @Feature method VoidFeatureConfig.f() must be " +
							"assignable to FeatureSpecification"));
		}
	}

	@Test
	public void containsBeanMethod() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(FeatureConfigWithBeanMethod.class);
		try {
			ctx.refresh();
			fail("expected exception");
		} catch (FeatureMethodExecutionException ex) {
			assertThat(ex.getMessage(),
					equalTo("@FeatureConfiguration classes must not contain @Bean-annotated methods. " +
							"FeatureConfigWithBeanMethod.testBean() is annotated with @Bean and must " +
							"be removed in order to proceed. Consider moving this method into a dedicated " +
							"@Configuration class and injecting the bean as a parameter into any @Feature " +
							"method(s) that need it."));
		}
	}


	@FeatureConfiguration
	static class FeatureConfig {
		@Feature
		public Object f() {
			return new StubSpecification();
		}
	}


	@FeatureConfiguration
	static class VoidFeatureConfig {
		@Feature
		public void f() {
		}
	}


	@FeatureConfiguration
	static class FeatureConfigWithBeanMethod {
		@Feature
		public FeatureSpecification f() {
			return new StubSpecification();
		}

		@Bean
		public TestBean testBean() {
			return new TestBean();
		}
	}

}
