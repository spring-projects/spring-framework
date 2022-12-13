/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.transaction.interceptor;

import java.io.IOException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.FatalBeanException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the {@link RollbackRuleAttribute} class.
 *
 * @author Rod Johnson
 * @author Rick Evans
 * @author Chris Beams
 * @author Sam Brannen
 * @since 09.04.2003
 */
class RollbackRuleAttributeTests {

	@Nested
	class ExceptionPatternTests {

		@Test
		void constructorPreconditions() {
			assertThatIllegalArgumentException().isThrownBy(() -> new RollbackRuleAttribute((String) null));
		}

		@Test
		void notFound() {
			RollbackRuleAttribute rr = new RollbackRuleAttribute(IOException.class.getName());
			assertThat(rr.getDepth(new MyRuntimeException())).isEqualTo(-1);
		}

		@Test
		void foundImmediatelyWhenDirectMatch() {
			RollbackRuleAttribute rr = new RollbackRuleAttribute(Exception.class.getName());
			assertThat(rr.getDepth(new Exception())).isEqualTo(0);
		}

		@Test
		void foundImmediatelyWhenExceptionThrownIsNestedTypeOfRegisteredException() {
			RollbackRuleAttribute rr = new RollbackRuleAttribute(EnclosingException.class.getName());
			assertThat(rr.getDepth(new EnclosingException.NestedException())).isEqualTo(0);
		}

		@Test
		void foundImmediatelyWhenNameOfExceptionThrownStartsWithNameOfRegisteredException() {
			// Precondition for this use case.
			assertThat(MyException.class.isAssignableFrom(MyException2.class)).isFalse();

			RollbackRuleAttribute rr = new RollbackRuleAttribute(MyException.class.getName());
			assertThat(rr.getDepth(new MyException2())).isEqualTo(0);
		}

		@Test
		void foundInSuperclassHierarchy() {
			RollbackRuleAttribute rr = new RollbackRuleAttribute(Exception.class.getName());
			// Exception -> RuntimeException -> NestedRuntimeException -> MyRuntimeException
			assertThat(rr.getDepth(new MyRuntimeException())).isEqualTo(3);
		}

		@Test
		void alwaysFoundForThrowable() {
			RollbackRuleAttribute rr = new RollbackRuleAttribute(Throwable.class.getName());
			assertThat(rr.getDepth(new MyRuntimeException())).isGreaterThan(0);
			assertThat(rr.getDepth(new IOException())).isGreaterThan(0);
			assertThat(rr.getDepth(new FatalBeanException(null, null))).isGreaterThan(0);
			assertThat(rr.getDepth(new RuntimeException())).isGreaterThan(0);
		}

	}

	@Nested
	class ExceptionTypeTests {

		@Test
		void constructorPreconditions() {
			assertThatIllegalArgumentException().isThrownBy(() -> new RollbackRuleAttribute(Object.class));
			assertThatIllegalArgumentException().isThrownBy(() -> new RollbackRuleAttribute((Class<?>) null));
		}

		@Test
		void notFound() {
			RollbackRuleAttribute rr = new RollbackRuleAttribute(IOException.class);
			assertThat(rr.getDepth(new MyRuntimeException())).isEqualTo(-1);
		}

		@Test
		void notFoundWhenNameOfExceptionThrownStartsWithNameOfRegisteredException() {
			// Precondition for this use case.
			assertThat(MyException.class.isAssignableFrom(MyException2.class)).isFalse();

			RollbackRuleAttribute rr = new RollbackRuleAttribute(MyException.class);
			assertThat(rr.getDepth(new MyException2())).isEqualTo(-1);
		}

		@Test
		void notFoundWhenExceptionThrownIsNestedTypeOfRegisteredException() {
			RollbackRuleAttribute rr = new RollbackRuleAttribute(EnclosingException.class);
			assertThat(rr.getDepth(new EnclosingException.NestedException())).isEqualTo(-1);
		}

		@Test
		void foundImmediatelyWhenDirectMatch() {
			RollbackRuleAttribute rr = new RollbackRuleAttribute(Exception.class);
			assertThat(rr.getDepth(new Exception())).isEqualTo(0);
		}

		@Test
		void foundInSuperclassHierarchy() {
			RollbackRuleAttribute rr = new RollbackRuleAttribute(Exception.class);
			// Exception -> RuntimeException -> NestedRuntimeException -> MyRuntimeException
			assertThat(rr.getDepth(new MyRuntimeException())).isEqualTo(3);
		}

		@Test
		void alwaysFoundForThrowable() {
			RollbackRuleAttribute rr = new RollbackRuleAttribute(Throwable.class);
			assertThat(rr.getDepth(new MyRuntimeException())).isGreaterThan(0);
			assertThat(rr.getDepth(new IOException())).isGreaterThan(0);
			assertThat(rr.getDepth(new FatalBeanException(null, null))).isGreaterThan(0);
			assertThat(rr.getDepth(new RuntimeException())).isGreaterThan(0);
		}

	}


	@SuppressWarnings("serial")
	static class EnclosingException extends RuntimeException {

		@SuppressWarnings("serial")
		static class NestedException extends RuntimeException {
		}
	}

	@SuppressWarnings("serial")
	static class MyException extends RuntimeException {
	}

	// Name intentionally starts with MyException (including package) but does
	// NOT extend MyException.
	@SuppressWarnings("serial")
	static class MyException2 extends RuntimeException {
	}

}
