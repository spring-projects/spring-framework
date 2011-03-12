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
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.context.annotation.configuration.StubSpecification;
import org.springframework.context.config.FeatureSpecification;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockEnvironment;

public class FeatureConfigurationClassTests {

	@Test(expected=FeatureMethodExecutionException.class)
	public void featureConfigurationClassesMustNotContainBeanAnnotatedMethods() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(FeatureConfigWithBeanAnnotatedMethod.class);
		ctx.refresh();
	}

	@Test
	public void featureMethodsMayAcceptResourceLoaderParameter() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.setDisplayName("enclosing app ctx");
		ctx.setEnvironment(new MockEnvironment().withProperty("foo", "bar"));
		ctx.register(FeatureMethodWithResourceLoaderParameter.class);
		ctx.refresh();
	}

}


@FeatureConfiguration
class FeatureConfigWithBeanAnnotatedMethod {
	/**
	 * This is illegal use. @FeatureConfiguration classes cannot have @Bean methods.
	 */
	@Bean
	public TestBean testBean() {
		return new TestBean();
	}

	/**
	 * This will never get called. An exception will first be raised regarding the illegal @Bean method above.
	 */
	@Feature
	public FeatureSpecification feature() {
		return new StubSpecification();
	}
}

@FeatureConfiguration
class FeatureMethodWithResourceLoaderParameter {
	@Feature
	public FeatureSpecification feature(ResourceLoader rl,
			Environment e) {
		// prove that the injected Environment is that of the enclosing app context
		assertThat(e.getProperty("foo"), is("bar"));
		// prove that the injected ResourceLoader is actually the enclosing application context
		Object target = ((EarlyBeanReferenceProxy)rl).dereferenceTargetBean();
		assertThat(target, instanceOf(AnnotationConfigApplicationContext.class));
		assertThat(((AnnotationConfigApplicationContext)target).getDisplayName(), is("enclosing app ctx"));
		return new StubSpecification();
	}
}