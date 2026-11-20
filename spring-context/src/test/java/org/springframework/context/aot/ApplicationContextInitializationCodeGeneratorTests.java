/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.aot;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.aot.generate.MethodReference.ArgumentCodeGenerator;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ApplicationContextInitializationCodeGenerator}.
 *
 * @author Stephane Nicoll
 */
class ApplicationContextInitializationCodeGeneratorTests {

	private static final ArgumentCodeGenerator argCodeGenerator = ApplicationContextInitializationCodeGenerator.
			createInitializerMethodArgumentCodeGenerator();

	@ParameterizedTest
	@MethodSource("methodArguments")
	void argumentsForSupportedTypesAreResolved(Class<?> target, String expectedArgument) {
		CodeBlock code = CodeBlock.of(expectedArgument);
		assertThat(argCodeGenerator.generateCode(ClassName.get(target))).isEqualTo(code);
	}

	@Test
	void argumentForUnsupportedBeanFactoryIsNotResolved() {
		assertThat(argCodeGenerator.generateCode(ClassName.get(AbstractBeanFactory.class))).isNull();
	}

	@Test
	void argumentForUnsupportedEnvironmentIsNotResolved() {
		assertThat(argCodeGenerator.generateCode(ClassName.get(StandardEnvironment.class))).isNull();
	}

	static Stream<Arguments> methodArguments() {
		String applicationContext = "applicationContext";
		String environment = applicationContext + ".getEnvironment()";
		return Stream.of(
				Arguments.of(DefaultListableBeanFactory.class, "beanFactory"),
				Arguments.of(ConfigurableListableBeanFactory.class, "beanFactory"),
				Arguments.of(ConfigurableEnvironment.class, environment),
				Arguments.of(Environment.class, environment),
				Arguments.of(ResourceLoader.class, applicationContext));
	}

}
