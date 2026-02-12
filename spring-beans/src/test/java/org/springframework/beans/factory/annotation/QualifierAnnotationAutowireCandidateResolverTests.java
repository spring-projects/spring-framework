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

package org.springframework.beans.factory.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.MethodParameter;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Unit tests for {@link QualifierAnnotationAutowireCandidateResolver}.
 *
 * @author Sam Brannen
 * @since 7.0.5
 */
class QualifierAnnotationAutowireCandidateResolverTests {

	final QualifierAnnotationAutowireCandidateResolver resolver = new QualifierAnnotationAutowireCandidateResolver();

	Method testMethod;

	@RegisterExtension
	BeforeTestExecutionCallback extension = context -> this.testMethod = context.getRequiredTestMethod();


	@Test
	void isNotAutowired() {
		assertRequired();
	}

	@Test
	void isAutowiredRequired() {
		assertRequired();
	}

	@Test
	void isAutowiredOptional() {
		assertNotRequired();
	}

	@Test
	void isMetaAutowiredRequired() {
		assertRequired();
	}

	@Test
	void isMetaAutowiredOptional() {
		assertNotRequired();
	}

	@Test
	void isMetaMetaAutowiredRequired() {
		assertRequired();
	}

	@Test
	void isMetaMetaAutowiredOptional() {
		assertNotRequired();
	}


	private void assertRequired() {
		assertSoftly(softly -> {
			softly.assertThat(this.resolver.isRequired(getFieldDescriptor()))
					.as("%sField is required", this.testMethod.getName()).isTrue();
			softly.assertThat(this.resolver.isRequired(getParameterDescriptor()))
					.as("parameter in %sParameter() is required", this.testMethod.getName()).isTrue();
		});
	}

	private void assertNotRequired() {
		assertSoftly(softly -> {
			softly.assertThat(this.resolver.isRequired(getFieldDescriptor()))
					.as("%sField is not required", this.testMethod.getName()).isFalse();
			softly.assertThat(this.resolver.isRequired(getParameterDescriptor()))
					.as("parameter in %sParameter() is not required", this.testMethod.getName()).isFalse();
		});
	}

	private DependencyDescriptor getFieldDescriptor() {
		var field = ReflectionUtils.findField(getClass(), this.testMethod.getName() + "Field");
		return new DependencyDescriptor(field, true);
	}

	private DependencyDescriptor getParameterDescriptor() {
		var method = ReflectionUtils.findMethod(getClass(), this.testMethod.getName() + "Parameter", String.class);
		var methodParameter = MethodParameter.forExecutable(method, 0);
		return new DependencyDescriptor(methodParameter, true);
	}


	String isNotAutowiredField;

	@Autowired
	String isAutowiredRequiredField;

	@Autowired(required = false)
	String isAutowiredOptionalField;

	@MetaAutowiredRequired
	String isMetaAutowiredRequiredField;

	@MetaAutowiredOptional
	String isMetaAutowiredOptionalField;

	@MetaMetaAutowiredRequired
	String isMetaMetaAutowiredRequiredField;

	@MetaMetaAutowiredOptional
	String isMetaMetaAutowiredOptionalField;



	void isNotAutowiredParameter(String enigma) {
	}

	void isAutowiredRequiredParameter(@Autowired String enigma) {
	}

	void isAutowiredOptionalParameter(@Autowired(required = false) String enigma) {
	}

	void isMetaAutowiredRequiredParameter(@MetaAutowiredRequired String enigma) {
	}

	void isMetaAutowiredOptionalParameter(@MetaAutowiredOptional String enigma) {
	}

	void isMetaMetaAutowiredRequiredParameter(@MetaMetaAutowiredRequired String enigma) {
	}

	void isMetaMetaAutowiredOptionalParameter(@MetaMetaAutowiredOptional String enigma) {
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Autowired
	@interface MetaAutowiredRequired {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Autowired(required = false)
	@interface MetaAutowiredOptional {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@MetaAutowiredRequired
	@interface MetaMetaAutowiredRequired {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@MetaAutowiredOptional
	@interface MetaMetaAutowiredOptional {
	}

}
