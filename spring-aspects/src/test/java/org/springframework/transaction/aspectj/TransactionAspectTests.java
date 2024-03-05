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

package org.springframework.transaction.aspectj;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

/**
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class TransactionAspectTests {

	private final CallCountingTransactionManager txManager = new CallCountingTransactionManager();

	private final TransactionalAnnotationOnlyOnClassWithNoInterface annotationOnlyOnClassWithNoInterface =
			new TransactionalAnnotationOnlyOnClassWithNoInterface();

	private final ClassWithProtectedAnnotatedMember beanWithAnnotatedProtectedMethod =
			new ClassWithProtectedAnnotatedMember();

	private final ClassWithPrivateAnnotatedMember beanWithAnnotatedPrivateMethod =
			new ClassWithPrivateAnnotatedMember();

	private final MethodAnnotationOnClassWithNoInterface methodAnnotationOnly =
			new MethodAnnotationOnClassWithNoInterface();


	@BeforeEach
	public void initContext() {
		AnnotationTransactionAspect.aspectOf().setTransactionManager(txManager);
	}


	@Test
	void testCommitOnAnnotatedClass() throws Throwable {
		txManager.clear();
		assertThat(txManager.begun).isEqualTo(0);
		annotationOnlyOnClassWithNoInterface.echo(null);
		assertThat(txManager.commits).isEqualTo(1);
	}

	@Test
	void commitOnAnnotatedProtectedMethod() {
		txManager.clear();
		assertThat(txManager.begun).isEqualTo(0);
		beanWithAnnotatedProtectedMethod.doInTransaction();
		assertThat(txManager.commits).isEqualTo(1);
	}

	@Test
	void commitOnAnnotatedPrivateMethod() {
		txManager.clear();
		assertThat(txManager.begun).isEqualTo(0);
		beanWithAnnotatedPrivateMethod.doSomething();
		assertThat(txManager.commits).isEqualTo(1);
	}

	@Test
	void commitOnNonAnnotatedNonPublicMethodInTransactionalType() {
		txManager.clear();
		assertThat(txManager.begun).isEqualTo(0);
		annotationOnlyOnClassWithNoInterface.nonTransactionalMethod();
		assertThat(txManager.begun).isEqualTo(0);
	}

	@Test
	void commitOnAnnotatedMethod() throws Throwable {
		txManager.clear();
		assertThat(txManager.begun).isEqualTo(0);
		methodAnnotationOnly.echo(null);
		assertThat(txManager.commits).isEqualTo(1);
	}

	@Test
	void notTransactional() {
		txManager.clear();
		assertThat(txManager.begun).isEqualTo(0);
		new NotTransactional().noop();
		assertThat(txManager.begun).isEqualTo(0);
	}

	@Test
	void defaultCommitOnAnnotatedClass() {
		Exception ex = new Exception();
		assertThatException()
				.isThrownBy(() -> testRollback(() -> annotationOnlyOnClassWithNoInterface.echo(ex), false))
				.isSameAs(ex);
	}

	@Test
	void defaultRollbackOnAnnotatedClass() {
		RuntimeException ex = new RuntimeException();
		assertThatRuntimeException()
				.isThrownBy(() -> testRollback(() -> annotationOnlyOnClassWithNoInterface.echo(ex), true))
				.isSameAs(ex);
	}

	@Test
	void defaultCommitOnSubclassOfAnnotatedClass() {
		Exception ex = new Exception();
		assertThatException()
				.isThrownBy(() -> testRollback(() -> new SubclassOfClassWithTransactionalAnnotation().echo(ex), false))
				.isSameAs(ex);
	}

	@Test
	void defaultCommitOnSubclassOfClassWithTransactionalMethodAnnotated() {
		Exception ex = new Exception();
		assertThatException()
				.isThrownBy(() -> testRollback(() -> new SubclassOfClassWithTransactionalMethodAnnotation().echo(ex), false))
				.isSameAs(ex);
	}

	@Test
	void noCommitOnImplementationOfAnnotatedInterface() {
		Exception ex = new Exception();
		testNotTransactional(() -> new ImplementsAnnotatedInterface().echo(ex), ex);
	}

	@Test
	void noRollbackOnImplementationOfAnnotatedInterface() {
		Exception rollbackProvokingException = new RuntimeException();
		testNotTransactional(() -> new ImplementsAnnotatedInterface().echo(rollbackProvokingException),
				rollbackProvokingException);
	}


	protected void testRollback(TransactionOperationCallback toc, boolean rollback) throws Throwable {
		txManager.clear();
		assertThat(txManager.begun).isEqualTo(0);
		try {
			toc.performTransactionalOperation();
		}
		finally {
			assertThat(txManager.begun).isEqualTo(1);
			long expected1 = (rollback ? 0 : 1);
			assertThat(txManager.commits).isEqualTo(expected1);
			long expected = (rollback ? 1 : 0);
			assertThat(txManager.rollbacks).isEqualTo(expected);
		}
	}

	protected void testNotTransactional(TransactionOperationCallback toc, Throwable expected) {
		txManager.clear();
		assertThat(txManager.begun).isEqualTo(0);
		assertThatExceptionOfType(Throwable.class)
				.isThrownBy(toc::performTransactionalOperation)
				.isSameAs(expected);
		assertThat(txManager.begun).isEqualTo(0);
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
