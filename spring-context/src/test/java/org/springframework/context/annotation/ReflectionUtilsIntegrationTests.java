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

package org.springframework.context.annotation;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests ReflectionUtils methods as used against CGLIB-generated classes created
 * by ConfigurationClassEnhancer.
 *
 * @author Chris Beams
 * @since 3.1
 * @see org.springframework.util.ReflectionUtilsTests
 */
class ReflectionUtilsIntegrationTests {

	@Test
	void getUniqueDeclaredMethods_withCovariantReturnType_andCglibRewrittenMethodNames() {
		Class<?> cglibLeaf = new ConfigurationClassEnhancer().enhance(Leaf.class, null);
		int m1MethodCount = 0;
		Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(cglibLeaf);
		for (Method method : methods) {
			if (method.getName().equals("m1")) {
				m1MethodCount++;
			}
		}
		assertThat(m1MethodCount).isEqualTo(1);
		for (Method method : methods) {
			if (method.getName().contains("m1")) {
				assertThat(Integer.class).isEqualTo(method.getReturnType());
			}
		}
	}


	@Configuration
	abstract static class Parent {
		public abstract Number m1();
	}


	@Configuration
	static class Leaf extends Parent {
		@Override
		@Bean
		public Integer m1() {
			return 42;
		}
	}

}
