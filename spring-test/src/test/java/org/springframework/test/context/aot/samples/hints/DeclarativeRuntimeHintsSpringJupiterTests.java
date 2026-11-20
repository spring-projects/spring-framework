/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot.samples.hints;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.test.context.aot.samples.hints.DeclarativeRuntimeHintsSpringJupiterTests.DemoHints;
import org.springframework.test.context.aot.samples.hints.DeclarativeRuntimeHintsSpringJupiterTests.SampleClassWithGetter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 6.0
 */
@SpringJUnitConfig
@Reflective
@RegisterReflectionForBinding(SampleClassWithGetter.class)
@ImportRuntimeHints(DemoHints.class)
public class DeclarativeRuntimeHintsSpringJupiterTests {

	@Test
	void test(@Autowired String foo) {
		assertThat(foo).isEqualTo("bar");
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		String foo() {
			return "bar";
		}
	}

	static class DemoHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.resources().registerPattern("org/example/config/*.txt");
		}

	}

	public static class SampleClassWithGetter {

		public String getName() {
			return null;
		}
	}

}
