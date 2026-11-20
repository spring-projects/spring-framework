/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.MethodParameter;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ContextAnnotationAutowireCandidateResolver}.
 *
 * @author Sam Brannen
 * @since 7.0.4
 */
class ContextAnnotationAutowireCandidateResolverTests {

	final ContextAnnotationAutowireCandidateResolver resolver = new ContextAnnotationAutowireCandidateResolver();

	Method testMethod;

	@RegisterExtension
	BeforeTestExecutionCallback extension = context -> this.testMethod = context.getRequiredTestMethod();


	@Test
	void isNotLazy() {
		assertNotLazy();
	}

	@Test
	void isLazy() {
		assertLazy();
	}

	@Test
	void isMetaLazy() {
		assertLazy();
	}

	@Test  // gh-36306
	void isMetaMetaLazy() {
		assertLazy();
	}

	private void assertLazy() {
		assertThat(this.resolver.isLazy(getMethodDescriptor()))
				.as("%sMethod() is @Lazy", this.testMethod.getName()).isTrue();
		assertThat(this.resolver.isLazy(getParameterDescriptor()))
				.as("parameter in %sParameter() is @Lazy", this.testMethod.getName()).isTrue();
	}

	private void assertNotLazy() {
		assertThat(this.resolver.isLazy(getMethodDescriptor()))
				.as("%sMethod() is not @Lazy", this.testMethod.getName()).isFalse();
		assertThat(this.resolver.isLazy(getParameterDescriptor()))
				.as("parameter in %sParameter() is not @Lazy", this.testMethod.getName()).isFalse();
	}

	private DependencyDescriptor getMethodDescriptor() {
		var method = ReflectionUtils.findMethod(getClass(), this.testMethod.getName() + "Method");
		var methodParameter = MethodParameter.forExecutable(method, -1);
		return new DependencyDescriptor(methodParameter, true);
	}

	private DependencyDescriptor getParameterDescriptor() {
		var method = ReflectionUtils.findMethod(getClass(), this.testMethod.getName() + "Parameter", String.class);
		var methodParameter = MethodParameter.forExecutable(method, 0);
		return new DependencyDescriptor(methodParameter, true);
	}


	void isNotLazyMethod() {
	}

	@Lazy
	void isLazyMethod() {
	}

	@MetaLazy
	void isMetaLazyMethod() {
	}

	@MetaMetaLazy
	void isMetaMetaLazyMethod() {
	}

	void isNotLazyParameter(String enigma) {
	}

	void isLazyParameter(@Lazy String enigma) {
	}

	void isMetaLazyParameter(@MetaLazy String enigma) {
	}

	void isMetaMetaLazyParameter(@MetaMetaLazy String enigma) {
	}


	@Lazy
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaLazy {
	}

	@MetaLazy
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaMetaLazy {
	}

}
