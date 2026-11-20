/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
