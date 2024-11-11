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

package org.springframework.test.web.servlet.assertj;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.assertj.core.api.AbstractMapAssert;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.assertj.core.internal.Failures;

import org.springframework.lang.Nullable;
import org.springframework.test.validation.AbstractBindingResultAssert;
import org.springframework.validation.BindingResult;
import org.springframework.validation.BindingResultUtils;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

/**
 * AssertJ {@linkplain org.assertj.core.api.Assert assertions} that can be applied
 * to a {@linkplain ModelAndView#getModel() model}.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
public class ModelAssert extends AbstractMapAssert<ModelAssert, Map<String, Object>, String, Object> {

	private final Failures failures = Failures.instance();

	public ModelAssert(Map<String, Object> map) {
		super(map, ModelAssert.class);
	}

	/**
	 * Return a new {@linkplain AbstractBindingResultAssert assertion} object
	 * that uses the {@link BindingResult} with the given {@code name} as the
	 * object to test.
	 * <p>Example: <pre><code class="java">
	 * // Check that the "person" attribute in the model has 2 errors:
	 * assertThat(...).model().extractingBindingResult("person").hasErrorsCount(2);
	 * </code></pre>
	 */
	public AbstractBindingResultAssert<?> extractingBindingResult(String name) {
		BindingResult result = BindingResultUtils.getBindingResult(this.actual, name);
		if (result == null) {
			throw unexpectedModel("to have a binding result for attribute '%s'", name);
		}
		return new BindingResultAssert(name, result);
	}

	/**
	 * Verify that the actual model has at least one error.
	 */
	public ModelAssert hasErrors() {
		if (getAllErrors() == 0) {
			throw unexpectedModel("to have at least one error");
		}
		return this.myself;
	}

	/**
	 * Verify that the actual model does not have any errors.
	 */
	public ModelAssert doesNotHaveErrors() {
		int count = getAllErrors(); if (count > 0) {
			throw unexpectedModel("to not have an error, but got %s", count);
		}
		return this.myself;
	}

	/**
	 * Verify that the actual model contains the attributes with the given
	 * {@code names}, and that each of these attributes has each at least one error.
	 * @param names the expected names of attributes with errors
	 */
	public ModelAssert hasAttributeErrors(String... names) {
		return assertAttributes(names, BindingResult::hasErrors,
				"to have attribute errors for", "these attributes do not have any errors");
	}

	/**
	 * Verify that the actual model contains the attributes with the given
	 * {@code names}, and that none of these attributes has an error.
	 * @param names the expected names of attributes without errors
	 */
	public ModelAssert doesNotHaveAttributeErrors(String... names) {
		return assertAttributes(names, Predicate.not(BindingResult::hasErrors),
				"to have attribute without errors for", "these attributes have at least one error");
	}

	private ModelAssert assertAttributes(String[] names, Predicate<BindingResult> condition,
			String assertionMessage, String failAssertionMessage) {

		Set<String> missing = new LinkedHashSet<>();
		Set<String> failCondition = new LinkedHashSet<>();
		for (String name : names) {
			BindingResult bindingResult = getBindingResult(name);
			if (bindingResult == null) {
				missing.add(name);
			}
			else if (!condition.test(bindingResult)) {
				failCondition.add(name);
			}
		}
		if (!missing.isEmpty() || !failCondition.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			sb.append("%n%s:%n  %s%n".formatted(assertionMessage, String.join(", ", names)));
			if (!missing.isEmpty()) {
				sb.append("%nbut could not find these attributes:%n  %s%n".formatted(String.join(", ", missing)));
			}
			if (!failCondition.isEmpty()) {
				String prefix = missing.isEmpty() ? "but" : "and";
				sb.append("%n%s %s:%n  %s%n".formatted(prefix, failAssertionMessage, String.join(", ", failCondition)));
			}
			throw unexpectedModel(sb.toString());
		}
		return this.myself;
	}

	private AssertionError unexpectedModel(String reason, Object... arguments) {
		return this.failures.failure(this.info, new UnexpectedModel(reason, arguments));
	}

	private int getAllErrors() {
		return this.actual.values().stream().filter(Errors.class::isInstance).map(Errors.class::cast)
				.map(Errors::getErrorCount).reduce(0, Integer::sum);
	}

	@Nullable
	private BindingResult getBindingResult(String name) {
		return BindingResultUtils.getBindingResult(this.actual, name);
	}

	private final class UnexpectedModel extends BasicErrorMessageFactory {

		private UnexpectedModel(String reason, Object... arguments) {
			super("%nExpecting model:%n  %s%n%s", ModelAssert.this.actual, reason.formatted(arguments));
		}
	}

	private static final class BindingResultAssert extends AbstractBindingResultAssert<BindingResultAssert> {
		public BindingResultAssert(String name, BindingResult bindingResult) {
			super(name, bindingResult, BindingResultAssert.class);
		}
	}

}
