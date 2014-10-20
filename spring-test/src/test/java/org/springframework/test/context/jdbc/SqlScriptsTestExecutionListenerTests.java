/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.test.context.jdbc;

import org.junit.Test;
import org.mockito.BDDMockito;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.jdbc.SqlConfig.TransactionMode;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link SqlScriptsTestExecutionListener}.
 *
 * @author Sam Brannen
 * @since 4.1
 */
public class SqlScriptsTestExecutionListenerTests {

	private final SqlScriptsTestExecutionListener listener = new SqlScriptsTestExecutionListener();

	private final TestContext testContext = mock(TestContext.class);


	@Test
	public void missingValueAndScriptsAtClassLevel() throws Exception {
		Class<?> clazz = MissingValueAndScriptsAtClassLevel.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("foo"));

		assertExceptionContains(clazz.getSimpleName() + ".sql");
	}

	@Test
	public void missingValueAndScriptsAtMethodLevel() throws Exception {
		Class<?> clazz = MissingValueAndScriptsAtMethodLevel.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("foo"));

		assertExceptionContains(clazz.getSimpleName() + ".foo" + ".sql");
	}

	@Test
	public void valueAndScriptsDeclared() throws Exception {
		Class<?> clazz = ValueAndScriptsDeclared.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("foo"));

		assertExceptionContains("Only one declaration of SQL script paths is permitted");
	}

	@Test
	public void isolatedTxModeDeclaredWithoutTxMgr() throws Exception {
		ApplicationContext ctx = mock(ApplicationContext.class);
		given(ctx.getResource(anyString())).willReturn(mock(Resource.class));
		given(ctx.getAutowireCapableBeanFactory()).willReturn(mock(AutowireCapableBeanFactory.class));

		Class<?> clazz = IsolatedWithoutTxMgr.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("foo"));
		given(testContext.getApplicationContext()).willReturn(ctx);

		assertExceptionContains("cannot execute SQL scripts using Transaction Mode [ISOLATED] without a PlatformTransactionManager");
	}

	@Test
	public void missingDataSourceAndTxMgr() throws Exception {
		ApplicationContext ctx = mock(ApplicationContext.class);
		given(ctx.getResource(anyString())).willReturn(mock(Resource.class));
		given(ctx.getAutowireCapableBeanFactory()).willReturn(mock(AutowireCapableBeanFactory.class));

		Class<?> clazz = MissingDataSourceAndTxMgr.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("foo"));
		given(testContext.getApplicationContext()).willReturn(ctx);

		assertExceptionContains("supply at least a DataSource or PlatformTransactionManager");
	}

	private void assertExceptionContains(String msg) throws Exception {
		try {
			listener.beforeTestMethod(testContext);
			fail("Should have thrown an IllegalStateException.");
		}
		catch (IllegalStateException e) {
			// System.err.println(e.getMessage());
			assertTrue("Exception message should contain: " + msg, e.getMessage().contains(msg));
		}
	}


	// -------------------------------------------------------------------------

	@Sql
	static class MissingValueAndScriptsAtClassLevel {

		public void foo() {
		}
	}

	static class MissingValueAndScriptsAtMethodLevel {

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

		@Sql(scripts = "foo.sql", config = @SqlConfig(transactionMode = TransactionMode.ISOLATED))
		public void foo() {
		}
	}

	static class MissingDataSourceAndTxMgr {

		@Sql("foo.sql")
		public void foo() {
		}
	}

}
