/*
 * Copyright 2002-2009 the original author or authors.
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

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConfigurationClassParser;
import org.springframework.context.annotation.Import;

import test.beans.TestBean;


/**
 * TCK-style unit tests for handling circular use of the {@link Import} annotation. Explore
 * subclass hierarchy for specific concrete implementations.
 *
 * @author Chris Beams
 */
public abstract class AbstractCircularImportDetectionTests {

	protected abstract ConfigurationClassParser newParser();

	protected abstract String loadAsConfigurationSource(Class<?> clazz) throws Exception;

	@Test
	public void simpleCircularImportIsDetected() throws Exception {
		boolean threw = false;
		try {
			newParser().parse(loadAsConfigurationSource(A.class), "A");
		} catch (BeanDefinitionParsingException ex) {
			assertTrue("Wrong message. Got: " + ex.getMessage(),
					ex.getMessage().contains(
						"Illegal attempt by @Configuration class 'AbstractCircularImportDetectionTests.B' " +
						"to import class 'AbstractCircularImportDetectionTests.A'"));
			threw = true;
		}

		assertTrue(threw);
	}


	@Test
	public void complexCircularImportIsDetected() throws Exception {
		boolean threw = false;
		try {
			newParser().parse(loadAsConfigurationSource(X.class), "X");
		}
		catch (BeanDefinitionParsingException ex) {
			assertTrue("Wrong message. Got: " + ex.getMessage(),
					ex.getMessage().contains(
						"Illegal attempt by @Configuration class 'AbstractCircularImportDetectionTests.Z2' " +
						"to import class 'AbstractCircularImportDetectionTests.Z'"));
			threw = true;
		}

		assertTrue(threw);
	}

	@Configuration
	@Import(B.class)
	static class A {
		@Bean
		TestBean b1() {
			return new TestBean();
		}
	}

	@Configuration
	@Import(A.class)
	static class B {
		@Bean
		TestBean b2() {
			return new TestBean();
		}
	}

	@Configuration
	@Import( { Y.class, Z.class })
	class X {
		@Bean
		TestBean x() {
			return new TestBean();
		}
	}

	@Configuration
	class Y {
		@Bean
		TestBean y() {
			return new TestBean();
		}
	}

	@Configuration
	@Import( { Z1.class, Z2.class })
	class Z {
		@Bean
		TestBean z() {
			return new TestBean();
		}
	}

	@Configuration
	class Z1 {
		@Bean
		TestBean z1() {
			return new TestBean();
		}
	}

	@Configuration
	@Import(Z.class)
	class Z2 {
		@Bean
		TestBean z2() {
			return new TestBean();
		}
	}

}
