/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.junit4;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.junit4.ContextCustomizerSpringRunnerTests.CustomTestContextBootstrapper;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;

import static java.util.Collections.*;
import static org.junit.Assert.*;

/**
 * JUnit 4 based integration test which verifies support of
 * {@link ContextCustomizerFactory} and {@link ContextCustomizer}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 4.3
 */
@RunWith(SpringRunner.class)
@BootstrapWith(CustomTestContextBootstrapper.class)
public class ContextCustomizerSpringRunnerTests {

	@Autowired String foo;


	@Test
	public void injectedBean() {
		assertEquals("foo", foo);
	}


	static class CustomTestContextBootstrapper extends DefaultTestContextBootstrapper {

		@Override
		protected List<ContextCustomizerFactory> getContextCustomizerFactories() {
			return singletonList(
				(ContextCustomizerFactory) (testClass, configAttributes) ->
					(ContextCustomizer) (context, mergedConfig) -> context.getBeanFactory().registerSingleton("foo", "foo")
			);
		}
	}

}
