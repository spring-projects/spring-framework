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

package org.springframework.test.context.jdbc;

import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationConfigurationException;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.TestContext;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.context.jdbc.SqlConfig.TransactionMode.ISOLATED;

/**
 * Tests for {@link SqlScriptsTestExecutionListener}.
 *
 * @author Sam Brannen
 * @author Andreas Ahlenstorf
 * @since 4.1
 */
class SqlScriptsTestExecutionListenerTests {

	private final SqlScriptsTestExecutionListener listener = new SqlScriptsTestExecutionListener();

	private final TestContext testContext = mock();


	@Test
	void missingValueAndScriptsAndStatementsAtClassLevel() throws Exception {
		Class<?> clazz = MissingValueAndScriptsAndStatementsAtClassLevel.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("foo"));

		assertExceptionContains(clazz.getSimpleName() + ".sql");
	}

	@Test
	void missingValueAndScriptsAndStatementsAtMethodLevel() throws Exception {
		Class<?> clazz = MissingValueAndScriptsAndStatementsAtMethodLevel.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("foo"));

		assertExceptionContains(clazz.getSimpleName() + ".foo" + ".sql");
	}

	@Test
	void valueAndScriptsDeclared() throws Exception {
		Class<?> clazz = ValueAndScriptsDeclared.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("foo"));

		assertThatExceptionOfType(AnnotationConfigurationException.class)
				.isThrownBy(() -> listener.beforeTestMethod(testContext))
				.withMessageContainingAll(
						"Different @AliasFor mirror values",
						"attribute 'scripts' and its alias 'value'",
						"values of [{bar}] and [{foo}]");
	}

	@Test
	void isolatedTxModeDeclaredWithoutTxMgr() throws Exception {
		ApplicationContext ctx = mock();
		given(ctx.getResource(anyString())).willReturn(mock());
		given(ctx.getAutowireCapableBeanFactory()).willReturn(mock());
		given(ctx.getEnvironment()).willReturn(new MockEnvironment());

		Class<?> clazz = IsolatedWithoutTxMgr.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("foo"));
		given(testContext.getApplicationContext()).willReturn(ctx);

		assertExceptionContains("cannot execute SQL scripts using Transaction Mode [ISOLATED] without a PlatformTransactionManager");
	}

	@Test
	void missingDataSourceAndTxMgr() throws Exception {
		ApplicationContext ctx = mock();
		given(ctx.getResource(anyString())).willReturn(mock());
		given(ctx.getAutowireCapableBeanFactory()).willReturn(mock());
		given(ctx.getEnvironment()).willReturn(new MockEnvironment());

		Class<?> clazz = MissingDataSourceAndTxMgr.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("foo"));
		given(testContext.getApplicationContext()).willReturn(ctx);

		assertExceptionContains("supply at least a DataSource or PlatformTransactionManager");
	}

	@Test
	void beforeTestClassOnMethod() throws Exception {
		Class<?> clazz = ClassLevelExecutionPhaseOnMethod.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("beforeTestClass"));

		assertThatIllegalArgumentException()
				.isThrownBy(() -> listener.beforeTestMethod(testContext))
				.withMessage("@SQL execution phase BEFORE_TEST_CLASS cannot be used on methods");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> listener.afterTestMethod(testContext))
				.withMessage("@SQL execution phase BEFORE_TEST_CLASS cannot be used on methods");
	}

	@Test
	void afterTestClassOnMethod() throws Exception {
		Class<?> clazz = ClassLevelExecutionPhaseOnMethod.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("afterTestClass"));

		assertThatIllegalArgumentException()
				.isThrownBy(() -> listener.beforeTestMethod(testContext))
				.withMessage("@SQL execution phase AFTER_TEST_CLASS cannot be used on methods");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> listener.afterTestMethod(testContext))
				.withMessage("@SQL execution phase AFTER_TEST_CLASS cannot be used on methods");
	}

	private void assertExceptionContains(String msg) {
		assertThatIllegalStateException()
				.isThrownBy(() -> listener.beforeTestMethod(testContext))
				.withMessageContaining(msg);
	}


	// -------------------------------------------------------------------------

	@Sql
	static class MissingValueAndScriptsAndStatementsAtClassLevel {

		public void foo() {
		}
	}

	static class MissingValueAndScriptsAndStatementsAtMethodLevel {

		@Sql
		public void foo() {
		}
	}

	static class ValueAndScriptsDeclared {

		@Sql(value = "foo", scripts = "bar")
		public void foo() {
		}
	}

	static class IsolatedWithoutTxMgr {

		@Sql(scripts = "foo.sql", config = @SqlConfig(transactionMode = ISOLATED))
		public void foo() {
		}
	}

	static class MissingDataSourceAndTxMgr {

		@Sql("foo.sql")
		public void foo() {
		}
	}

	static class ClassLevelExecutionPhaseOnMethod {

		@Sql(scripts = "foo.sql", executionPhase = BEFORE_TEST_CLASS)
		public void beforeTestClass() {
		}

		@Sql(scripts = "foo.sql", executionPhase = AFTER_TEST_CLASS)
		public void afterTestClass() {
		}
	}

}
