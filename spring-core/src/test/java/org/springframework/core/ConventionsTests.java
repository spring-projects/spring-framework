/*
 * Copyright 2002-2018 the original author or authors.
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

import io.reactivex.Observable;
import io.reactivex.Single;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.tests.sample.objects.TestObject;
import org.springframework.util.ClassUtils;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link Conventions}.
 *
 * @author Rob Harrop
 * @author Sam Brannen
 */
public class ConventionsTests {

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void simpleObject() {
		assertEquals("Incorrect singular variable name",
				"testObject", Conventions.getVariableName(new TestObject()));
		assertEquals("Incorrect singular variable name", "testObject",
				Conventions.getVariableNameForParameter(getMethodParameter(TestObject.class)));
		assertEquals("Incorrect singular variable name", "testObject",
				Conventions.getVariableNameForReturnType(getMethodForReturnType(TestObject.class)));
	}

	@Test
	public void array() {
		assertEquals("Incorrect plural array form",
				"testObjectList", Conventions.getVariableName(new TestObject[0]));
	}

	@Test
	public void list() {
		assertEquals("Incorrect plural List form", "testObjectList",
				Conventions.getVariableName(Collections.singletonList(new TestObject())));
		assertEquals("Incorrect plural List form", "testObjectList",
				Conventions.getVariableNameForParameter(getMethodParameter(List.class)));
		assertEquals("Incorrect plural List form", "testObjectList",
				Conventions.getVariableNameForReturnType(getMethodForReturnType(List.class)));
	}

	@Test
	public void emptyList() {
		this.exception.expect(IllegalArgumentException.class);
		Conventions.getVariableName(new ArrayList<>());
	}

	@Test
	public void set() {
		assertEquals("Incorrect plural Set form", "testObjectList",
				Conventions.getVariableName(Collections.singleton(new TestObject())));
		assertEquals("Incorrect plural Set form", "testObjectList",
				Conventions.getVariableNameForParameter(getMethodParameter(Set.class)));
		assertEquals("Incorrect plural Set form", "testObjectList",
				Conventions.getVariableNameForReturnType(getMethodForReturnType(Set.class)));
	}

	@Test
	public void reactiveParameters() {
		assertEquals("testObjectMono",
				Conventions.getVariableNameForParameter(getMethodParameter(Mono.class)));
		assertEquals("testObjectFlux",
				Conventions.getVariableNameForParameter(getMethodParameter(Flux.class)));
		assertEquals("testObjectSingle",
				Conventions.getVariableNameForParameter(getMethodParameter(Single.class)));
		assertEquals("testObjectObservable",
				Conventions.getVariableNameForParameter(getMethodParameter(Observable.class)));
	}

	@Test
	public void reactiveReturnTypes() {
		assertEquals("testObjectMono",
				Conventions.getVariableNameForReturnType(getMethodForReturnType(Mono.class)));
		assertEquals("testObjectFlux",
				Conventions.getVariableNameForReturnType(getMethodForReturnType(Flux.class)));
		assertEquals("testObjectSingle",
				Conventions.getVariableNameForReturnType(getMethodForReturnType(Single.class)));
		assertEquals("testObjectObservable",
				Conventions.getVariableNameForReturnType(getMethodForReturnType(Observable.class)));
	}

	@Test
	public void attributeNameToPropertyName() {
		assertEquals("transactionManager", Conventions.attributeNameToPropertyName("transaction-manager"));
		assertEquals("pointcutRef", Conventions.attributeNameToPropertyName("pointcut-ref"));
		assertEquals("lookupOnStartup", Conventions.attributeNameToPropertyName("lookup-on-startup"));
	}

	@Test
	public void getQualifiedAttributeName() {
		String baseName = "foo";
		Class<String> cls = String.class;
		String desiredResult = "java.lang.String.foo";
		assertEquals(desiredResult, Conventions.getQualifiedAttributeName(cls, baseName));
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
