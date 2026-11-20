/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
