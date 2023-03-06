/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aot.test.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.assertj.core.error.ErrorMessageFactory;

import org.springframework.aot.agent.RecordedInvocation;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;

/**
 * AssertJ {@link org.assertj.core.api.Assert assertions} that can be applied to
 * {@link RuntimeHintsInvocations}.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public class RuntimeHintsInvocationsAssert extends AbstractAssert<RuntimeHintsInvocationsAssert, RuntimeHintsInvocations> {

	private final List<Consumer<RuntimeHints>> configurers = new ArrayList<>();

	RuntimeHintsInvocationsAssert(RuntimeHintsInvocations invocations) {
		super(invocations, RuntimeHintsInvocationsAssert.class);
	}

	public RuntimeHintsInvocationsAssert withRegistrar(RuntimeHintsRegistrar registrar) {
		this.configurers.add(hints -> registrar.registerHints(hints, getClass().getClassLoader()));
		return this;
	}

	public RuntimeHintsInvocationsAssert withSpringFactoriesRegistrars(String location) {
		List<RuntimeHintsRegistrar> registrars = SpringFactoriesLoader.forResourceLocation(location).load(RuntimeHintsRegistrar.class);
		this.configurers.add(hints -> registrars.forEach(registrar -> registrar.registerHints(hints, getClass().getClassLoader())));
		return this;
	}

	private void configureRuntimeHints(RuntimeHints hints) {
		this.configurers.forEach(configurer -> configurer.accept(hints));
	}

	/**
	 * Verifies that each recorded invocation match at least once hint in the provided {@link RuntimeHints}.
	 * <p>
	 * Example: <pre class="code">
	 * RuntimeHints hints = new RuntimeHints();
	 * hints.reflection().registerType(MyType.class);
	 * assertThat(invocations).match(hints); </pre>
	 * @param runtimeHints the runtime hints configuration to test against
	 * @throws AssertionError if any of the recorded invocations has no match in the provided hints
	 */
	public void match(RuntimeHints runtimeHints) {
		Assert.notNull(runtimeHints, "RuntimeHints must not be null");
		configureRuntimeHints(runtimeHints);
		List<RecordedInvocation> noMatchInvocations =
				this.actual.recordedInvocations().filter(invocation -> !invocation.matches(runtimeHints)).toList();
		if (!noMatchInvocations.isEmpty()) {
			throwAssertionError(errorMessageForInvocation(noMatchInvocations.get(0)));
		}
	}

	public ListAssert<RecordedInvocation> notMatching(RuntimeHints runtimeHints) {
		Assert.notNull(runtimeHints, "RuntimeHints must not be null");
		configureRuntimeHints(runtimeHints);
		return ListAssert.assertThatStream(this.actual.recordedInvocations()
				.filter(invocation -> !invocation.matches(runtimeHints)));
	}


	private ErrorMessageFactory errorMessageForInvocation(RecordedInvocation invocation) {
		if (invocation.isStatic()) {
			return new BasicErrorMessageFactory("%nMissing <%s> for invocation <%s>%nwith arguments %s.%nStacktrace:%n<%s>",
					invocation.getHintType().hintClassName(), invocation.getMethodReference(),
					invocation.getArguments(), formatStackTrace(invocation.getStackFrames()));
		}
		else {
			Class<?> instanceType = (invocation.getInstance() instanceof Class<?> clazz) ? clazz : invocation.getInstance().getClass();
			return new BasicErrorMessageFactory("%nMissing <%s> for invocation <%s> on type <%s> %nwith arguments %s.%nStacktrace:%n<%s>",
					invocation.getHintType().hintClassName(), invocation.getMethodReference(),
					instanceType, invocation.getArguments(),
					formatStackTrace(invocation.getStackFrames()));
		}
	}

	private String formatStackTrace(Stream<StackWalker.StackFrame> stackTraceElements) {
		return stackTraceElements
				.map(f -> f.getClassName() + "#" + f.getMethodName()
						+ ", Line " + f.getLineNumber()).collect(Collectors.joining(System.lineSeparator()));
	}

	/**
	 * Verifies that the count of recorded invocations match the expected one.
	 * <p>
	 * Example: <pre class="code">
	 * assertThat(invocations).hasCount(42); </pre>
	 * @param count the expected invocations count
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the number of recorded invocations doesn't match the expected one
	 */
	public RuntimeHintsInvocationsAssert hasCount(long count) {
		isNotNull();
		long invocationsCount = this.actual.recordedInvocations().count();
		if(invocationsCount != count) {
			throwAssertionError(new BasicErrorMessageFactory("%nNumber of recorded invocations does not match, expected <%n> but got <%n>.",
					invocationsCount, count));
		}
		return this;
	}

}
