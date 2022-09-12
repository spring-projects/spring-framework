/*
 * Copyright 2002-2022 the original author or authors.
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
