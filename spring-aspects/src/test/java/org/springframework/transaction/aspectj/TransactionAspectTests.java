/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.transaction.aspectj;

import org.junit.Before;
import org.junit.Test;

import org.springframework.tests.transaction.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;

/**
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class TransactionAspectTests {

	private final CallCountingTransactionManager txManager = new CallCountingTransactionManager();

	private final TransactionalAnnotationOnlyOnClassWithNoInterface annotationOnlyOnClassWithNoInterface =
			new TransactionalAnnotationOnlyOnClassWithNoInterface();

	private final ClassWithProtectedAnnotatedMember beanWithAnnotatedProtectedMethod =
			new ClassWithProtectedAnnotatedMember();

	private final ClassWithPrivateAnnotatedMember beanWithAnnotatedPrivateMethod =
			new ClassWithPrivateAnnotatedMember();

	private final MethodAnnotationOnClassWithNoInterface methodAnnotationOnly =
			new MethodAnnotationOnClassWithNoInterface();


	@Before
	public void initContext() {
		AnnotationTransactionAspect.aspectOf().setTransactionManager(txManager);
	}


	@Test
	public void testCommitOnAnnotatedClass() throws Throwable {
		txManager.clear();
		assertEquals(0, txManager.begun);
		annotationOnlyOnClassWithNoInterface.echo(null);
		assertEquals(1, txManager.commits);
	}

	@Test
	public void commitOnAnnotatedProtectedMethod() throws Throwable {
		txManager.clear();
		assertEquals(0, txManager.begun);
		beanWithAnnotatedProtectedMethod.doInTransaction();
		assertEquals(1, txManager.commits);
	}

	@Test
	public void commitOnAnnotatedPrivateMethod() throws Throwable {
		txManager.clear();
		assertEquals(0, txManager.begun);
		beanWithAnnotatedPrivateMethod.doSomething();
		assertEquals(1, txManager.commits);
	}

	@Test
	public void commitOnNonAnnotatedNonPublicMethodInTransactionalType() throws Throwable {
		txManager.clear();
		assertEquals(0, txManager.begun);
		annotationOnlyOnClassWithNoInterface.nonTransactionalMethod();
		assertEquals(0, txManager.begun);
	}

	@Test
	public void commitOnAnnotatedMethod() throws Throwable {
		txManager.clear();
		assertEquals(0, txManager.begun);
		methodAnnotationOnly.echo(null);
		assertEquals(1, txManager.commits);
	}

	@Test
	public void notTransactional() throws Throwable {
		txManager.clear();
		assertEquals(0, txManager.begun);
		new NotTransactional().noop();
		assertEquals(0, txManager.begun);
	}

	@Test
	public void defaultCommitOnAnnotatedClass() throws Throwable {
		Exception ex = new Exception();
		assertThatExceptionOfType(Exception.class).isThrownBy(() ->
				testRollback(() -> annotationOnlyOnClassWithNoInterface.echo(ex), false))
			.isSameAs(ex);
	}

	@Test
	public void defaultRollbackOnAnnotatedClass() throws Throwable {
		RuntimeException ex = new RuntimeException();
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
				testRollback(() -> annotationOnlyOnClassWithNoInterface.echo(ex), true))
			.isSameAs(ex);
	}

	@Test
	public void defaultCommitOnSubclassOfAnnotatedClass() throws Throwable {
		Exception ex = new Exception();
		assertThatExceptionOfType(Exception.class).isThrownBy(() ->
				testRollback(() -> new SubclassOfClassWithTransactionalAnnotation().echo(ex), false))
			.isSameAs(ex);
	}

	@Test
	public void defaultCommitOnSubclassOfClassWithTransactionalMethodAnnotated() throws Throwable {
		Exception ex = new Exception();
		assertThatExceptionOfType(Exception.class).isThrownBy(() ->
				testRollback(() -> new SubclassOfClassWithTransactionalMethodAnnotation().echo(ex), false))
			.isSameAs(ex);
	}

	@Test
	public void noCommitOnImplementationOfAnnotatedInterface() throws Throwable {
		final Exception ex = new Exception();
		testNotTransactional(() -> new ImplementsAnnotatedInterface().echo(ex), ex);
	}

	@Test
	public void noRollbackOnImplementationOfAnnotatedInterface() throws Throwable {
		final Exception rollbackProvokingException = new RuntimeException();
		testNotTransactional(() -> new ImplementsAnnotatedInterface().echo(rollbackProvokingException),
				rollbackProvokingException);
	}


	protected void testRollback(TransactionOperationCallback toc, boolean rollback) throws Throwable {
		txManager.clear();
		assertEquals(0, txManager.begun);
		try {
			toc.performTransactionalOperation();
		}
		finally {
			assertEquals(1, txManager.begun);
			assertEquals(rollback ? 0 : 1, txManager.commits);
			assertEquals(rollback ? 1 : 0, txManager.rollbacks);
		}
	}

	protected void testNotTransactional(TransactionOperationCallback toc, Throwable expected) throws Throwable {
		txManager.clear();
		assertEquals(0, txManager.begun);
		assertThatExceptionOfType(Throwable.class).isThrownBy(
				toc::performTransactionalOperation).isSameAs(expected);
		assertEquals(0, txManager.begun);
	}


	private interface TransactionOperationCallback {

		Object performTransactionalOperation() throws Throwable;
	}


	public static class SubclassOfClassWithTransactionalAnnotation extends TransactionalAnnotationOnlyOnClassWithNoInterface {
	}


	public static class SubclassOfClassWithTransactionalMethodAnnotation extends MethodAnnotationOnClassWithNoInterface {
	}


	public static class ImplementsAnnotatedInterface implements ITransactional {

		@Override
		public Object echo(Throwable t) throws Throwable {
			if (t != null) {
				throw t;
			}
			return t;
		}
	}


	public static class NotTransactional {

		public void noop() {
		}
	}

}
