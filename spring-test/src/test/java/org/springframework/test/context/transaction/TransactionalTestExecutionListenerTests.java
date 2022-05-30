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

package org.springframework.test.context.transaction;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;

import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * Unit tests for {@link TransactionalTestExecutionListener}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class TransactionalTestExecutionListenerTests {

	private final PlatformTransactionManager tm = mock(PlatformTransactionManager.class);

	private final TransactionalTestExecutionListener listener = new TransactionalTestExecutionListener() {
		@Override
		protected PlatformTransactionManager getTransactionManager(TestContext testContext, String qualifier) {
			return tm;
		}
	};

	private final TestContext testContext = mock(TestContext.class);


	@AfterEach
	void cleanUpThreadLocalStateForSubsequentTestClassesInSuite() {
		TransactionContextHolder.removeCurrentTransactionContext();
	}


	@Test  // SPR-13895
	void transactionalTestWithoutTransactionManager() throws Exception {
		TransactionalTestExecutionListener listener = new TransactionalTestExecutionListener() {
			@Override
			protected PlatformTransactionManager getTransactionManager(TestContext testContext, String qualifier) {
				return null;
			}
		};

		Class<? extends Invocable> clazz = TransactionalDeclaredOnClassLocallyTestCase.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		Invocable instance = BeanUtils.instantiateClass(clazz);
		given(testContext.getTestInstance()).willReturn(instance);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("transactionalTest"));

		assertThat(instance.invoked()).as("callback should not have been invoked").isFalse();
		TransactionContextHolder.removeCurrentTransactionContext();

		assertThatIllegalStateException().isThrownBy(() ->
				listener.beforeTestMethod(testContext))
			.withMessageStartingWith("Failed to retrieve PlatformTransactionManager for @Transactional test");
	}

	@Test
	void beforeTestMethodWithTransactionalDeclaredOnClassLocally() throws Exception {
		assertBeforeTestMethodWithTransactionalTestMethod(TransactionalDeclaredOnClassLocallyTestCase.class);
	}

	@Test
	void beforeTestMethodWithTransactionalDeclaredOnClassViaMetaAnnotation() throws Exception {
		assertBeforeTestMethodWithTransactionalTestMethod(TransactionalDeclaredOnClassViaMetaAnnotationTestCase.class);
	}

	@Test
	void beforeTestMethodWithTransactionalDeclaredOnClassViaMetaAnnotationWithOverride() throws Exception {
		// Note: not actually invoked within a transaction since the test class is
		// annotated with @MetaTxWithOverride(propagation = NOT_SUPPORTED)
		assertBeforeTestMethodWithTransactionalTestMethod(
				TransactionalDeclaredOnClassViaMetaAnnotationWithOverrideTestCase.class, false);
	}

	@Test
	void beforeTestMethodWithTransactionalDeclaredOnMethodViaMetaAnnotationWithOverride() throws Exception {
		// Note: not actually invoked within a transaction since the method is
		// annotated with @MetaTxWithOverride(propagation = NOT_SUPPORTED)
		assertBeforeTestMethodWithTransactionalTestMethod(
				TransactionalDeclaredOnMethodViaMetaAnnotationWithOverrideTestCase.class, false);
		assertBeforeTestMethodWithNonTransactionalTestMethod(TransactionalDeclaredOnMethodViaMetaAnnotationWithOverrideTestCase.class);
	}

	@Test
	void beforeTestMethodWithTransactionalDeclaredOnMethodLocally() throws Exception {
		assertBeforeTestMethod(TransactionalDeclaredOnMethodLocallyTestCase.class);
	}

	@Test
	void beforeTestMethodWithTransactionalDeclaredOnMethodViaMetaAnnotation() throws Exception {
		assertBeforeTestMethod(TransactionalDeclaredOnMethodViaMetaAnnotationTestCase.class);
	}

	@Test
	void beforeTestMethodWithBeforeTransactionDeclaredLocally() throws Exception {
		assertBeforeTestMethod(BeforeTransactionDeclaredLocallyTestCase.class);
	}

	@Test
	void beforeTestMethodWithBeforeTransactionDeclaredViaMetaAnnotation() throws Exception {
		assertBeforeTestMethod(BeforeTransactionDeclaredViaMetaAnnotationTestCase.class);
	}

	@Test
	void afterTestMethodWithAfterTransactionDeclaredLocally() throws Exception {
		assertAfterTestMethod(AfterTransactionDeclaredLocallyTestCase.class);
	}

	@Test
	void afterTestMethodWithAfterTransactionDeclaredViaMetaAnnotation() throws Exception {
		assertAfterTestMethod(AfterTransactionDeclaredViaMetaAnnotationTestCase.class);
	}

	@Test
	void beforeTestMethodWithBeforeTransactionDeclaredAsInterfaceDefaultMethod() throws Exception {
		assertBeforeTestMethod(BeforeTransactionDeclaredAsInterfaceDefaultMethodTestCase.class);
	}

	@Test
	void afterTestMethodWithAfterTransactionDeclaredAsInterfaceDefaultMethod() throws Exception {
		assertAfterTestMethod(AfterTransactionDeclaredAsInterfaceDefaultMethodTestCase.class);
	}

	@Test
	void isRollbackWithMissingRollback() throws Exception {
		assertIsRollback(MissingRollbackTestCase.class, true);
	}

	@Test
	void isRollbackWithEmptyMethodLevelRollback() throws Exception {
		assertIsRollback(EmptyMethodLevelRollbackTestCase.class, true);
	}

	@Test
	void isRollbackWithMethodLevelRollbackWithExplicitValue() throws Exception {
		assertIsRollback(MethodLevelRollbackWithExplicitValueTestCase.class, false);
	}

	@Test
	void isRollbackWithMethodLevelRollbackViaMetaAnnotation() throws Exception {
		assertIsRollback(MethodLevelRollbackViaMetaAnnotationTestCase.class, false);
	}

	@Test
	void isRollbackWithEmptyClassLevelRollback() throws Exception {
		assertIsRollback(EmptyClassLevelRollbackTestCase.class, true);
	}

	@Test
	void isRollbackWithClassLevelRollbackWithExplicitValue() throws Exception {
		assertIsRollback(ClassLevelRollbackWithExplicitValueTestCase.class, false);
	}

	@Test
	void isRollbackWithClassLevelRollbackViaMetaAnnotation() throws Exception {
		assertIsRollback(ClassLevelRollbackViaMetaAnnotationTestCase.class, false);
	}

	@Test
	void isRollbackWithClassLevelRollbackWithExplicitValueOnTestInterface() throws Exception {
		assertIsRollback(ClassLevelRollbackWithExplicitValueOnTestInterfaceTestCase.class, false);
	}

	@Test
	void isRollbackWithClassLevelRollbackViaMetaAnnotationOnTestInterface() throws Exception {
		assertIsRollback(ClassLevelRollbackViaMetaAnnotationOnTestInterfaceTestCase.class, false);
	}


	private void assertBeforeTestMethod(Class<? extends Invocable> clazz) throws Exception {
		assertBeforeTestMethodWithTransactionalTestMethod(clazz);
		assertBeforeTestMethodWithNonTransactionalTestMethod(clazz);
	}

	private void assertBeforeTestMethodWithTransactionalTestMethod(Class<? extends Invocable> clazz) throws Exception {
		assertBeforeTestMethodWithTransactionalTestMethod(clazz, true);
	}

	private void assertBeforeTestMethodWithTransactionalTestMethod(Class<? extends Invocable> clazz, boolean invokedInTx)
			throws Exception {

		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		Invocable instance = BeanUtils.instantiateClass(clazz);
		given(testContext.getTestInstance()).willReturn(instance);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("transactionalTest"));

		assertThat(instance.invoked()).as("callback should not have been invoked").isFalse();
		TransactionContextHolder.removeCurrentTransactionContext();
		listener.beforeTestMethod(testContext);
		assertThat(instance.invoked()).isEqualTo(invokedInTx);
	}

	private void assertBeforeTestMethodWithNonTransactionalTestMethod(Class<? extends Invocable> clazz) throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		Invocable instance = BeanUtils.instantiateClass(clazz);
		given(testContext.getTestInstance()).willReturn(instance);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("nonTransactionalTest"));

		assertThat(instance.invoked()).as("callback should not have been invoked").isFalse();
		TransactionContextHolder.removeCurrentTransactionContext();
		listener.beforeTestMethod(testContext);
		assertThat(instance.invoked()).as("callback should not have been invoked").isFalse();
	}

	private void assertAfterTestMethod(Class<? extends Invocable> clazz) throws Exception {
		assertAfterTestMethodWithTransactionalTestMethod(clazz);
		assertAfterTestMethodWithNonTransactionalTestMethod(clazz);
	}

	private void assertAfterTestMethodWithTransactionalTestMethod(Class<? extends Invocable> clazz) throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		Invocable instance = BeanUtils.instantiateClass(clazz);
		given(testContext.getTestInstance()).willReturn(instance);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("transactionalTest"));
		given(tm.getTransaction(BDDMockito.any(TransactionDefinition.class))).willReturn(new SimpleTransactionStatus());

		assertThat(instance.invoked()).as("callback should not have been invoked").isFalse();
		TransactionContextHolder.removeCurrentTransactionContext();
		listener.beforeTestMethod(testContext);
		assertThat(instance.invoked()).as("callback should not have been invoked").isFalse();
		listener.afterTestMethod(testContext);
		assertThat(instance.invoked()).as("callback should have been invoked").isTrue();
	}

	private void assertAfterTestMethodWithNonTransactionalTestMethod(Class<? extends Invocable> clazz) throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		Invocable instance = BeanUtils.instantiateClass(clazz);
		given(testContext.getTestInstance()).willReturn(instance);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("nonTransactionalTest"));

		assertThat(instance.invoked()).as("callback should not have been invoked").isFalse();
		TransactionContextHolder.removeCurrentTransactionContext();
		listener.beforeTestMethod(testContext);
		listener.afterTestMethod(testContext);
		assertThat(instance.invoked()).as("callback should not have been invoked").isFalse();
	}

	private void assertIsRollback(Class<?> clazz, boolean rollback) throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("test"));
		assertThat(listener.isRollback(testContext)).isEqualTo(rollback);
	}


	@Transactional
	@Retention(RetentionPolicy.RUNTIME)
	private @interface MetaTransactional {
	}

	@Transactional
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface MetaTxWithOverride {

		@AliasFor(annotation = Transactional.class, attribute = "value")
		String transactionManager() default "";

		Propagation propagation() default REQUIRED;
	}

	@BeforeTransaction
	@Retention(RetentionPolicy.RUNTIME)
	private @interface MetaBeforeTransaction {
	}

	@AfterTransaction
	@Retention(RetentionPolicy.RUNTIME)
	private @interface MetaAfterTransaction {
	}

	private interface Invocable {

		void invoked(boolean invoked);

		boolean invoked();
	}

	private static class AbstractInvocable implements Invocable {

		boolean invoked = false;


		@Override
		public void invoked(boolean invoked) {
			this.invoked = invoked;
		}

		@Override
		public boolean invoked() {
			return this.invoked;
		}
	}

	@Transactional
	static class TransactionalDeclaredOnClassLocallyTestCase extends AbstractInvocable {

		@BeforeTransaction
		public void beforeTransaction() {
			invoked(true);
		}

		public void transactionalTest() {
		}
	}

	static class TransactionalDeclaredOnMethodLocallyTestCase extends AbstractInvocable {

		@BeforeTransaction
		public void beforeTransaction() {
			invoked(true);
		}

		@Transactional
		public void transactionalTest() {
		}

		public void nonTransactionalTest() {
		}
	}

	@MetaTransactional
	static class TransactionalDeclaredOnClassViaMetaAnnotationTestCase extends AbstractInvocable {

		@BeforeTransaction
		public void beforeTransaction() {
			invoked(true);
		}

		public void transactionalTest() {
		}
	}

	static class TransactionalDeclaredOnMethodViaMetaAnnotationTestCase extends AbstractInvocable {

		@BeforeTransaction
		public void beforeTransaction() {
			invoked(true);
		}

		@MetaTransactional
		public void transactionalTest() {
		}

		public void nonTransactionalTest() {
		}
	}

	@MetaTxWithOverride(propagation = NOT_SUPPORTED)
	static class TransactionalDeclaredOnClassViaMetaAnnotationWithOverrideTestCase extends AbstractInvocable {

		@BeforeTransaction
		public void beforeTransaction() {
			invoked(true);
		}

		public void transactionalTest() {
		}
	}

	static class TransactionalDeclaredOnMethodViaMetaAnnotationWithOverrideTestCase extends AbstractInvocable {

		@BeforeTransaction
		public void beforeTransaction() {
			invoked(true);
		}

		@MetaTxWithOverride(propagation = NOT_SUPPORTED)
		public void transactionalTest() {
		}

		public void nonTransactionalTest() {
		}
	}

	static class BeforeTransactionDeclaredLocallyTestCase extends AbstractInvocable {

		@BeforeTransaction
		public void beforeTransaction() {
			invoked(true);
		}

		@Transactional
		public void transactionalTest() {
		}

		public void nonTransactionalTest() {
		}
	}

	static class BeforeTransactionDeclaredViaMetaAnnotationTestCase extends AbstractInvocable {

		@MetaBeforeTransaction
		public void beforeTransaction() {
			invoked(true);
		}

		@Transactional
		public void transactionalTest() {
		}

		public void nonTransactionalTest() {
		}
	}

	static class AfterTransactionDeclaredLocallyTestCase extends AbstractInvocable {

		@AfterTransaction
		public void afterTransaction() {
			invoked(true);
		}

		@Transactional
		public void transactionalTest() {
		}

		public void nonTransactionalTest() {
		}
	}

	static class AfterTransactionDeclaredViaMetaAnnotationTestCase extends AbstractInvocable {

		@MetaAfterTransaction
		public void afterTransaction() {
			invoked(true);
		}

		@Transactional
		public void transactionalTest() {
		}

		public void nonTransactionalTest() {
		}
	}

	interface BeforeTransactionInterface extends Invocable {

		@BeforeTransaction
		default void beforeTransaction() {
			invoked(true);
		}
	}

	interface AfterTransactionInterface extends Invocable {

		@AfterTransaction
		default void afterTransaction() {
			invoked(true);
		}
	}

	static class BeforeTransactionDeclaredAsInterfaceDefaultMethodTestCase extends AbstractInvocable
			implements BeforeTransactionInterface {

		@Transactional
		public void transactionalTest() {
		}

		public void nonTransactionalTest() {
		}
	}

	static class AfterTransactionDeclaredAsInterfaceDefaultMethodTestCase extends AbstractInvocable
			implements AfterTransactionInterface {

		@Transactional
		public void transactionalTest() {
		}

		public void nonTransactionalTest() {
		}
	}

	static class MissingRollbackTestCase {

		public void test() {
		}
	}

	static class EmptyMethodLevelRollbackTestCase {

		@Rollback
		public void test() {
		}
	}

	static class MethodLevelRollbackWithExplicitValueTestCase {

		@Rollback(false)
		public void test() {
		}
	}

	static class MethodLevelRollbackViaMetaAnnotationTestCase {

		@Commit
		public void test() {
		}
	}

	@Rollback
	static class EmptyClassLevelRollbackTestCase {

		public void test() {
		}
	}

	@Rollback(false)
	static class ClassLevelRollbackWithExplicitValueTestCase {

		public void test() {
		}
	}

	@Commit
	static class ClassLevelRollbackViaMetaAnnotationTestCase {

		public void test() {
		}
	}

	@Rollback(false)
	interface RollbackFalseTestInterface {
	}

	static class ClassLevelRollbackWithExplicitValueOnTestInterfaceTestCase implements RollbackFalseTestInterface {

		public void test() {
		}
	}

	@Commit
	interface RollbackFalseViaMetaAnnotationTestInterface {
	}

	static class ClassLevelRollbackViaMetaAnnotationOnTestInterfaceTestCase
			implements RollbackFalseViaMetaAnnotationTestInterface {

		public void test() {
		}
	}

}
