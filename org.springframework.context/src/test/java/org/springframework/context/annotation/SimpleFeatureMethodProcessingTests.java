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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.context.annotation.configuration.StubSpecification;
import org.springframework.context.config.ExecutorContext;
import org.springframework.context.config.FeatureSpecification;
import org.springframework.context.config.FeatureSpecificationExecutor;
import org.springframework.util.Assert;

/**
 * Simple tests to ensure that @Feature methods are invoked and that the
 * resulting returned {@link FeatureSpecification} object is delegated to
 * the correct {@link FeatureSpecificationExecutor}.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class SimpleFeatureMethodProcessingTests {

	@Test
	public void test() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(FeatureConfig.class);
		assertThat(MySpecificationExecutor.executeMethodWasCalled, is(false));
		ctx.refresh();
		assertThat(MySpecificationExecutor.executeMethodWasCalled, is(true));
	}

	@FeatureConfiguration
	static class FeatureConfig {
		@Feature
		public FeatureSpecification f() {
			return new StubSpecification(MySpecificationExecutor.class);
		}
	}

	static class MySpecificationExecutor implements FeatureSpecificationExecutor {
		static boolean executeMethodWasCalled = false;
		public void execute(FeatureSpecification spec, ExecutorContext executorContext) {
			Assert.state(executeMethodWasCalled == false);
			executeMethodWasCalled = true;
		}
	}

}
