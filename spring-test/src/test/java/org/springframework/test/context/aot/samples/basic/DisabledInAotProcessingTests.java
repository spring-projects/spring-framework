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

package org.springframework.test.context.aot.samples.basic;

import org.junit.jupiter.api.Test;

import org.springframework.aot.AotDetector;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.aot.AotTestExecutionListener;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.aot.TestContextAotGenerator;
import org.springframework.test.context.aot.samples.basic.DisabledInAotProcessingTests.BrokenAotTestExecutionListener;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

/**
 * {@code @DisabledInAotMode} test class which verifies that the application context
 * for the test class is skipped during AOT processing and that an
 * {@link AotTestExecutionListener} is also not invoked.
 *
 * @author Sam Brannen
 * @since 6.1
 */
@SpringJUnitConfig
@DisabledInAotMode
@TestExecutionListeners(listeners = BrokenAotTestExecutionListener.class, mergeMode = MERGE_WITH_DEFAULTS)
public class DisabledInAotProcessingTests {

	@Test
	void disabledInAotMode(@Autowired String enigma) {
		assertThat(AotDetector.useGeneratedArtifacts()).as("Should be disabled in AOT mode").isFalse();
		assertThat(enigma).isEqualTo("puzzle");
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		String enigma() {
			return "puzzle";
		}

		@Bean
		static BeanFactoryPostProcessor bfppBrokenDuringAotProcessing() {
			boolean runningDuringAotProcessing = StackWalker.getInstance().walk(stream ->
					stream.anyMatch(stackFrame -> stackFrame.getClassName().equals(TestContextAotGenerator.class.getName())));

			return beanFactory -> Assert.state(!runningDuringAotProcessing, "Should not be used during AOT processing");
		}
	}

	static class BrokenAotTestExecutionListener implements AotTestExecutionListener {

		@Override
		public void processAheadOfTime(RuntimeHints runtimeHints, Class<?> testClass, ClassLoader classLoader) {
			throw new UnsupportedOperationException("Broken AotTestExecutionListener");
		}
	}

}
