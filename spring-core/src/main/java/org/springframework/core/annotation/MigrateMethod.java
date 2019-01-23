/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.springframework.core.annotation.InternalAnnotationUtils.DefaultValueHolder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Internal class used to help migrate annotation util methods to a new implementation.
 *
 * @author Phillip Webb
 * @since 5.2
 */
final class MigrateMethod {

	private MigrateMethod() {
	}

	/**
	 * Create a new {@link ReplacementMethod} builder for the deprecated method.
	 * @param originalMethod the original method being migrated
	 * @return a replacement builder.
	 */
	static <T> ReplacementMethod<T> from(Supplier<T> originalMethod) {
		return new ReplacementMethod<>(originalMethod);
	}

	/**
	 * Create a new {@link ReplacementVoidMethod} for the deprecated method.
	 * @param originalMethod the original method being migrated
	 * @return a replacement builder.
	 */
	static ReplacementVoidMethod fromCall(Runnable originalMethod) {
		return new ReplacementVoidMethod(originalMethod);
	}

	private static boolean isEquivalent(@Nullable Object result, @Nullable  Object expectedResult) {
		if (ObjectUtils.nullSafeEquals(result, expectedResult)) {
			return true;
		}
		if (result == null && String.valueOf(expectedResult).startsWith(
				"@org.springframework.lang.")) {
			// Original methods don't filter spring annotation but we do
			return true;
		}
		if (result == null || expectedResult == null) {
			return false;
		}
		if (result instanceof DefaultValueHolder && expectedResult instanceof DefaultValueHolder) {
			return isEquivalent(((DefaultValueHolder) result).defaultValue,
					((DefaultValueHolder) expectedResult).defaultValue);
		}
		if (result instanceof Map && expectedResult instanceof Map) {
			return isEquivalentMap((Map<?, ?>) result, (Map<?, ?>) expectedResult);
		}
		if (result instanceof List && expectedResult instanceof List) {
			return isEquivalentList((List<?>) result, (List<?>) expectedResult);
		}
		if (result instanceof Object[] && expectedResult instanceof Object[]) {
			return isEquivalentArray((Object[]) result, (Object[]) expectedResult);
		}
		if (result instanceof Object[]) {
			if (isEquivalentArray((Object[]) result, new Object[] { expectedResult })) {
				return true;
			}
		}
		if (!(result instanceof Object[]) && expectedResult instanceof Object[]) {
			if (isEquivalentArray(new Object[] { result }, (Object[]) expectedResult)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isEquivalentMap(Map<?, ?> result, Map<?, ?> expectedResult) {
		if (result.size() != expectedResult.size()) {
			return false;
		}
		for (Map.Entry<?, ?> entry : result.entrySet()) {
			if (!expectedResult.containsKey(entry.getKey())) {
				return false;
			}
			if (!isEquivalent(entry.getValue(), expectedResult.get(entry.getKey()))) {
				return false;
			}
		}
		return true;
	}

	private static boolean isEquivalentList(List<?> result, List<?> expectedResult) {
		if (result.size() != expectedResult.size()) {
			return false;
		}
		for (int i = 0; i < result.size(); i++) {
			if (!isEquivalent(result.get(i), expectedResult.get(i))) {
				return false;
			}
		}
		return true;
	}

	private static boolean isEquivalentArray(Object[] result, Object[] expectedResult) {
		if (result.length != expectedResult.length) {
			return false;
		}
		for (int i = 0; i < result.length; i++) {
			if (!isEquivalent(result[i], expectedResult[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Builder to complete replacement details for a deprecated annotation method.
	 * @param <T> the return type
	 */
	static class ReplacementMethod<T> {

		private final Supplier<T> originalMethod;

		@Nullable
		private Supplier<String> description;

		private boolean skipOriginalExceptionCheck;

		private BooleanSupplier skipEquivalentCheck = () -> false;

		ReplacementMethod(Supplier<T> deprecatedMethod) {
			this.originalMethod = deprecatedMethod;
		}

		/**
		 * Add a description for the method.
		 * @param description a description supplier
		 * @return this instance
		 */
		public ReplacementMethod<T> withDescription(Supplier<String> description) {
			this.description = description;
			return this;
		}

		public ReplacementMethod<T> withSkippedOriginalExceptionCheck() {
			this.skipOriginalExceptionCheck = true;
			return this;
		}

		public ReplacementMethod<T> withSkippedEquivalentCheck(BooleanSupplier supplier) {
			this.skipEquivalentCheck = supplier;
			return this;
		}

		/**
		 * Provide the replacement method that should be used instead of the deprecated
		 * one. The replacement method is called, and when appropriate the result is
		 * checked against the deprecated method.
		 * @param replacementMethod the replacement method
		 * @return the result of the replacement method
		 */
		public T to(Supplier<T> replacementMethod) {
			T result = toNullable(replacementMethod);
			if (result == null) {
				throw new IllegalStateException("Unexpected null result");
			}
			return result;
		}

		/**
		 * Provide the replacement method that should be used instead of the deprecated
		 * one. The replacement method is called, and when appropriate the result is
		 * checked against the deprecated method.
		 * @param replacementMethod the replacement method
		 * @return the result of the replacement method
		 */
		@Nullable
		public T toNullable(Supplier<T> replacementMethod) {
			T result = tryInvoke(replacementMethod);
			T expectedResult = this.originalMethod.get();
			if (!isEquivalent(result, expectedResult)) {
				if (this.skipEquivalentCheck.getAsBoolean()) {
					return expectedResult;
				}
				String description = (this.description != null ? " [" +
							this.description.get() + "]" : "");
				throw new IllegalStateException("Expected " + expectedResult +
						" got " + result + description);
			}
			return result;
		}

		private T tryInvoke(Supplier<T> replacementMethod) {
			try {
				return replacementMethod.get();
			}
			catch (RuntimeException expected) {
				try {
					T expectedResult = this.originalMethod.get();
					if (this.skipOriginalExceptionCheck) {
						return expectedResult;
					}
					throw new Error("Expected exception not thrown", expected);
				}
				catch (RuntimeException actual) {
					if (!expected.getClass().isInstance(actual)) {
						throw new Error(
								"Exception is not " + expected.getClass().getName(),
								actual);
					}
					throw actual;
				}
			}
		}

	}

	/**
	 * Builder to complete replacement details for a deprecated annotation method that
	 * returns void.
	 */
	static class ReplacementVoidMethod {

		private final Runnable originalMethod;

		private final List<Object[]> argumentChecks = new ArrayList<>();

		public ReplacementVoidMethod(Runnable originalMethod) {
			this.originalMethod = originalMethod;
		}

		public ReplacementVoidMethod withArgumentCheck(Object originalArgument,
				Object replacementArgument) {
			this.argumentChecks.add(
					new Object[] { originalArgument, replacementArgument });
			return this;
		}

		public void to(Runnable replacementMethod) {
			tryInvoke(this.originalMethod);
			replacementMethod.run();
			for (Object[] arguments : this.argumentChecks) {
				Object expectedArgument = arguments[0];
				Object actualArgument = arguments[1];
				Assert.state(isEquivalent(actualArgument, expectedArgument),
						() -> "Expected argument mutation of " + expectedArgument
								+ " got " + actualArgument);
			}
		}

		private void tryInvoke(Runnable replacementMethod) {
			try {
				replacementMethod.run();
			}
			catch (RuntimeException expected) {
				try {
					this.originalMethod.run();
					throw new Error("Expected exception not thrown", expected);
				}
				catch (RuntimeException actual) {
					if (!expected.getClass().isInstance(actual)) {
						throw new Error(
								"Exception is not " + expected.getClass().getName(),
								actual);
					}
					throw actual;
				}
			}
		}

	}

}
