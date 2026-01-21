/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.context.aot;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.reflection;

/**
 * Tests for registering run-time hints within an {@link AotContextLoader}, tested
 * via the {@link TestContextAotGenerator}.
 *
 * @author Sam Brannen
 * @since 6.2.4
 */
class AotContextLoaderRuntimeHintsTests {

	@Test  // gh-34513
	void aotContextLoaderCanRegisterRuntimeHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		TestContextAotGenerator generator = new TestContextAotGenerator(new InMemoryGeneratedFiles(), runtimeHints);

		generator.processAheadOfTime(Stream.of(TestCase.class));

		assertThat(reflection().onMethodInvocation(ConfigWithMain.class, "main")).accepts(runtimeHints);
	}


	@ContextConfiguration(classes = ConfigWithMain.class, loader = RuntimeHintsAwareAotContextLoader.class)
	static class TestCase {
	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigWithMain {

		public static void main(String[] args) {
			// Mimics main() method for Spring Boot app
		}
	}

	static class RuntimeHintsAwareAotContextLoader extends AbstractAotContextLoader {

		@Override
		public void processContextConfiguration(ContextConfigurationAttributes configAttributes) {
			/* no-op */
		}

		@Override
		public GenericApplicationContext loadContextForAotProcessing(MergedContextConfiguration mergedConfig,
				RuntimeHints runtimeHints) throws Exception {

			// Mimics SpringBootContextLoader
			Method mainMethod = mergedConfig.getClasses()[0].getMethod("main", String[].class);
			runtimeHints.reflection().registerMethod(mainMethod, ExecutableMode.INVOKE);

			return loadContext(mergedConfig);
		}
	}

}
