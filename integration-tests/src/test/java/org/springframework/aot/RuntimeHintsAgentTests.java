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

package org.springframework.aot;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.aot.agent.HintType;
import org.springframework.aot.agent.MethodReference;
import org.springframework.aot.agent.RecordedInvocation;
import org.springframework.aot.agent.RecordedInvocationsListener;
import org.springframework.aot.agent.RecordedInvocationsPublisher;
import org.springframework.aot.agent.RuntimeHintsAgent;
import org.springframework.aot.test.agent.EnabledIfRuntimeHintsAgent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link RuntimeHintsAgent}.
 *
 * @author Brian Clozel
 */
@EnabledIfRuntimeHintsAgent
class RuntimeHintsAgentTests {

	private static final ClassLoader classLoader = ClassLoader.getSystemClassLoader();

	private static Constructor<String> defaultConstructor;

	private static Method toStringMethod;

	private static Method privateGreetMethod;


	@BeforeAll
	static void classSetup() throws NoSuchMethodException {
		defaultConstructor = String.class.getConstructor();
		toStringMethod = String.class.getMethod("toString");
		privateGreetMethod = PrivateClass.class.getDeclaredMethod("greet");
	}


	@ParameterizedTest
	@MethodSource("instrumentedReflectionMethods")
	void shouldInstrumentReflectionMethods(Runnable runnable, MethodReference methodReference) {
		RecordingSession session = RecordingSession.record(runnable);
		assertThat(session.recordedInvocations()).hasSize(1);
		RecordedInvocation invocation = session.recordedInvocations().findFirst().get();
		assertThat(invocation.getMethodReference()).isEqualTo(methodReference);
		assertThat(invocation.getStackFrames()).first().matches(frame -> frame.getClassName().equals(RuntimeHintsAgentTests.class.getName()));
	}

	private static Stream<Arguments> instrumentedReflectionMethods() {
		return Stream.of(
				Arguments.of((Runnable) () -> {
					try {
						Class.forName("java.lang.String");
					}
					catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				}, MethodReference.of(Class.class, "forName")),
				Arguments.of((Runnable) () -> String.class.getClasses(), MethodReference.of(Class.class, "getClasses")),
				Arguments.of((Runnable) () -> {
					try {
						String.class.getConstructor();
					}
					catch (NoSuchMethodException e) {
						throw new RuntimeException(e);
					}
				}, MethodReference.of(Class.class, "getConstructor")),
				Arguments.of((Runnable) () -> String.class.getConstructors(), MethodReference.of(Class.class, "getConstructors")),
				Arguments.of((Runnable) () -> String.class.getDeclaredClasses(), MethodReference.of(Class.class, "getDeclaredClasses")),
				Arguments.of((Runnable) () -> {
					try {
						String.class.getDeclaredConstructor();
					}
					catch (NoSuchMethodException e) {
						throw new RuntimeException(e);
					}
				}, MethodReference.of(Class.class, "getDeclaredConstructor")),
				Arguments.of((Runnable) () -> String.class.getDeclaredConstructors(), MethodReference.of(Class.class, "getDeclaredConstructors")),
				Arguments.of((Runnable) () -> {
					try {
						String.class.getDeclaredField("value");
					}
					catch (NoSuchFieldException e) {
						throw new RuntimeException(e);
					}
				}, MethodReference.of(Class.class, "getDeclaredField")),
				Arguments.of((Runnable) () -> String.class.getDeclaredFields(), MethodReference.of(Class.class, "getDeclaredFields")),
				Arguments.of((Runnable) () -> {
					try {
						String.class.getDeclaredMethod("toString");
					}
					catch (NoSuchMethodException e) {
						throw new RuntimeException(e);
					}
				}, MethodReference.of(Class.class, "getDeclaredMethod")),
				Arguments.of((Runnable) () -> String.class.getDeclaredMethods(), MethodReference.of(Class.class, "getDeclaredMethods")),
				Arguments.of((Runnable) () -> {
					try {
						String.class.getField("value");
					}
					catch (NoSuchFieldException e) {
					}
				}, MethodReference.of(Class.class, "getField")),
				Arguments.of((Runnable) () -> String.class.getFields(), MethodReference.of(Class.class, "getFields")),
				Arguments.of((Runnable) () -> {
					try {
						String.class.getMethod("toString");
					}
					catch (NoSuchMethodException e) {
						throw new RuntimeException(e);
					}
				}, MethodReference.of(Class.class, "getMethod")),
				Arguments.of((Runnable) () -> String.class.getMethods(), MethodReference.of(Class.class, "getMethods")),
				Arguments.of((Runnable) () -> {
					try {
						classLoader.loadClass("java.lang.String");
					}
					catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				}, MethodReference.of(ClassLoader.class, "loadClass")),
				Arguments.of((Runnable) () -> {
					try {
						defaultConstructor.newInstance();
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
				}, MethodReference.of(Constructor.class, "newInstance")),
				Arguments.of((Runnable) () -> {
					try {
						toStringMethod.invoke("");
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
				}, MethodReference.of(Method.class, "invoke")),
				Arguments.of((Runnable) () -> {
					try {
						privateGreetMethod.invoke(new PrivateClass());
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
				}, MethodReference.of(Method.class, "invoke"))
		);
	}

	@ParameterizedTest
	@MethodSource("instrumentedResourceBundleMethods")
	void shouldInstrumentResourceBundleMethods(Runnable runnable, MethodReference methodReference) {
		RecordingSession session = RecordingSession.record(runnable);
		assertThat(session.recordedInvocations(HintType.RESOURCE_BUNDLE)).hasSize(1);

		RecordedInvocation resolution = session.recordedInvocations(HintType.RESOURCE_BUNDLE).findFirst().get();
		assertThat(resolution.getMethodReference()).isEqualTo(methodReference);
		assertThat(resolution.getStackFrames()).first().matches(frame -> frame.getClassName().equals(RuntimeHintsAgentTests.class.getName()));
	}


	private static Stream<Arguments> instrumentedResourceBundleMethods() {
		return Stream.of(
				Arguments.of((Runnable) () -> {
							try {
								ResourceBundle.getBundle("testBundle");
							}
							catch (Throwable exc) {
							}
						},
						MethodReference.of(ResourceBundle.class, "getBundle"))
		);
	}

	@ParameterizedTest
	@MethodSource("instrumentedResourcePatternMethods")
	void shouldInstrumentResourcePatternMethods(Runnable runnable, MethodReference methodReference) {
		RecordingSession session = RecordingSession.record(runnable);
		assertThat(session.recordedInvocations(HintType.RESOURCE_PATTERN)).hasSize(1);

		RecordedInvocation resolution = session.recordedInvocations(HintType.RESOURCE_PATTERN).findFirst().get();
		assertThat(resolution.getMethodReference()).isEqualTo(methodReference);
		assertThat(resolution.getStackFrames()).first().matches(frame -> frame.getClassName().equals(RuntimeHintsAgentTests.class.getName()));
	}


	private static Stream<Arguments> instrumentedResourcePatternMethods() {
		return Stream.of(
				Arguments.of((Runnable) () -> RuntimeHintsAgentTests.class.getResource("sample.txt"),
						MethodReference.of(Class.class, "getResource")),
				Arguments.of((Runnable) () -> RuntimeHintsAgentTests.class.getResourceAsStream("sample.txt"),
						MethodReference.of(Class.class, "getResourceAsStream")),
				Arguments.of((Runnable) () -> classLoader.getResource("sample.txt"),
						MethodReference.of(ClassLoader.class, "getResource")),
				Arguments.of((Runnable) () -> classLoader.getResourceAsStream("sample.txt"),
						MethodReference.of(ClassLoader.class, "getResourceAsStream")),
				Arguments.of((Runnable) () -> {
							try {
								classLoader.getResources("sample.txt");
							}
							catch (IOException e) {
							}
						},
						MethodReference.of(ClassLoader.class, "getResources")),
				Arguments.of((Runnable) () -> {
							try {
								RuntimeHintsAgentTests.class.getModule().getResourceAsStream("sample.txt");
							}
							catch (IOException e) {
							}
						},
						MethodReference.of(Module.class, "getResourceAsStream")),
				Arguments.of((Runnable) () -> classLoader.resources("sample.txt"),
						MethodReference.of(ClassLoader.class, "resources"))
		);
	}

	@Test
	void shouldInstrumentStaticMethodHandle() {
		RecordingSession session = RecordingSession.record(ClassLoader.class::getClasses);
		assertThat(session.recordedInvocations(HintType.REFLECTION)).hasSize(1);

		RecordedInvocation resolution = session.recordedInvocations(HintType.REFLECTION).findFirst().get();
		assertThat(resolution.getMethodReference()).isEqualTo(MethodReference.of(Class.class, "getClasses"));
		assertThat(resolution.getStackFrames()).first().extracting(StackWalker.StackFrame::getClassName)
				.isEqualTo(RuntimeHintsAgentTests.class.getName() + "$RecordingSession");
	}

	static class RecordingSession implements RecordedInvocationsListener {

		final Deque<RecordedInvocation> recordedInvocations = new ArrayDeque<>();

		static RecordingSession record(Runnable action) {
			RecordingSession session = new RecordingSession();
			RecordedInvocationsPublisher.addListener(session);
			try {
				action.run();
			}
			finally {
				RecordedInvocationsPublisher.removeListener(session);
			}
			return session;
		}

		@Override
		public void onInvocation(RecordedInvocation invocation) {
			this.recordedInvocations.addLast(invocation);
		}

		Stream<RecordedInvocation> recordedInvocations() {
			return this.recordedInvocations.stream();
		}

		Stream<RecordedInvocation> recordedInvocations(HintType hintType) {
			return recordedInvocations().filter(invocation -> invocation.getHintType() == hintType);
		}

	}

	private static class PrivateClass {

		@SuppressWarnings("unused")
		private String greet() {
			return "hello";
		}

	}

}
