/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.tests.sample.objects.TestObject;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Conventions}.
 *
 * @author Rob Harrop
 * @author Sam Brannen
 */
class ConventionsTests {

	@Test
	void simpleObject() {
		assertThat(Conventions.getVariableName(new TestObject())).as("Incorrect singular variable name").isEqualTo("testObject");
		assertThat(Conventions.getVariableNameForParameter(getMethodParameter(TestObject.class))).as("Incorrect singular variable name").isEqualTo("testObject");
		assertThat(Conventions.getVariableNameForReturnType(getMethodForReturnType(TestObject.class))).as("Incorrect singular variable name").isEqualTo("testObject");
	}

	@Test
	void array() {
		Object actual = Conventions.getVariableName(new TestObject[0]);
		assertThat(actual).as("Incorrect plural array form").isEqualTo("testObjectList");
	}

	@Test
	void list() {
		assertThat(Conventions.getVariableName(Collections.singletonList(new TestObject()))).as("Incorrect plural List form").isEqualTo("testObjectList");
		assertThat(Conventions.getVariableNameForParameter(getMethodParameter(List.class))).as("Incorrect plural List form").isEqualTo("testObjectList");
		assertThat(Conventions.getVariableNameForReturnType(getMethodForReturnType(List.class))).as("Incorrect plural List form").isEqualTo("testObjectList");
	}

	@Test
	void emptyList() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Conventions.getVariableName(new ArrayList<>()));
	}

	@Test
	void set() {
		assertThat(Conventions.getVariableName(Collections.singleton(new TestObject()))).as("Incorrect plural Set form").isEqualTo("testObjectList");
		assertThat(Conventions.getVariableNameForParameter(getMethodParameter(Set.class))).as("Incorrect plural Set form").isEqualTo("testObjectList");
		assertThat(Conventions.getVariableNameForReturnType(getMethodForReturnType(Set.class))).as("Incorrect plural Set form").isEqualTo("testObjectList");
	}

	@Test
	void reactiveParameters() {
		assertThat(Conventions.getVariableNameForParameter(getMethodParameter(Mono.class))).isEqualTo("testObjectMono");
		assertThat(Conventions.getVariableNameForParameter(getMethodParameter(Flux.class))).isEqualTo("testObjectFlux");
		assertThat(Conventions.getVariableNameForParameter(getMethodParameter(Single.class))).isEqualTo("testObjectSingle");
		assertThat(Conventions.getVariableNameForParameter(getMethodParameter(Observable.class))).isEqualTo("testObjectObservable");
	}

	@Test
	void reactiveReturnTypes() {
		assertThat(Conventions.getVariableNameForReturnType(getMethodForReturnType(Mono.class))).isEqualTo("testObjectMono");
		assertThat(Conventions.getVariableNameForReturnType(getMethodForReturnType(Flux.class))).isEqualTo("testObjectFlux");
		assertThat(Conventions.getVariableNameForReturnType(getMethodForReturnType(Single.class))).isEqualTo("testObjectSingle");
		assertThat(Conventions.getVariableNameForReturnType(getMethodForReturnType(Observable.class))).isEqualTo("testObjectObservable");
	}

	@Test
	void attributeNameToPropertyName() {
		assertThat(Conventions.attributeNameToPropertyName("transaction-manager")).isEqualTo("transactionManager");
		assertThat(Conventions.attributeNameToPropertyName("pointcut-ref")).isEqualTo("pointcutRef");
		assertThat(Conventions.attributeNameToPropertyName("lookup-on-startup")).isEqualTo("lookupOnStartup");
	}

	@Test
	void getQualifiedAttributeName() {
		String baseName = "foo";
		Class<String> cls = String.class;
		String desiredResult = "java.lang.String.foo";
		assertThat(Conventions.getQualifiedAttributeName(cls, baseName)).isEqualTo(desiredResult);
	}


	private static MethodParameter getMethodParameter(Class<?> parameterType) {
		Method method = ClassUtils.getMethod(TestBean.class, "handle", (Class<?>[]) null);
		for (int i=0; i < method.getParameterCount(); i++) {
			if (parameterType.equals(method.getParameterTypes()[i])) {
				return new MethodParameter(method, i);
			}
		}
		throw new IllegalArgumentException("Parameter type not found: " + parameterType);
	}

	private static Method getMethodForReturnType(Class<?> returnType) {
		return Arrays.stream(TestBean.class.getMethods())
				.filter(method -> method.getReturnType().equals(returnType))
				.findFirst()
				.orElseThrow(() ->
						new IllegalArgumentException("Unique return type not found: " + returnType));
	}


	@SuppressWarnings("unused")
	private static class TestBean {

		public void handle(TestObject to,
				List<TestObject> toList, Set<TestObject> toSet,
				Mono<TestObject> toMono, Flux<TestObject> toFlux,
				Single<TestObject> toSingle, Observable<TestObject> toObservable) { }

		public TestObject handleTo() { return null; }

		public List<TestObject> handleToList() { return null; }

		public Set<TestObject> handleToSet() { return null; }

		public Mono<TestObject> handleToMono() { return null; }

		public Flux<TestObject> handleToFlux() { return null; }

		public Single<TestObject> handleToSingle() { return null; }

		public Observable<TestObject> handleToObservable() { return null; }

	}

}
