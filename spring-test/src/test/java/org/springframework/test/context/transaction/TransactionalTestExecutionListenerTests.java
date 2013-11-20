/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.test.context.transaction;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.transaction.annotation.Propagation.*;

/**
 * Unit tests for {@link TransactionalTestExecutionListener}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class TransactionalTestExecutionListenerTests {

	private final PlatformTransactionManager tm = mock(PlatformTransactionManager.class);

	private final TransactionalTestExecutionListener listener = new TransactionalTestExecutionListener() {

		protected PlatformTransactionManager getTransactionManager(TestContext testContext, String qualifier) {
			return tm;
		}
	};

	private final TestContext testContext = mock(TestContext.class);


	private void assertBeforeTestMethod(Class<? extends Invocable> clazz) throws Exception {
		assertBeforeTestMethodWithTransactionalTestMethod(clazz);
		assertBeforeTestMethodWithNonTransactionalTestMethod(clazz);
	}

	private void assertBeforeTestMethodWithTransactionalTestMethod(Class<? extends Invocable> clazz) throws Exception {
		assertBeforeTestMethodWithTransactionalTestMethod(clazz, true);
	}

	private void assertBeforeTestMethodWithTransactionalTestMethod(Class<? extends Invocable> clazz, boolean invokedInTx)
			throws Exception {
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(clazz);
		Invocable instance = clazz.newInstance();
		when(testContext.getTestInstance()).thenReturn(instance);
		when(testContext.getTestMethod()).thenReturn(clazz.getDeclaredMethod("transactionalTest"));

		assertFalse(instance.invoked);
		listener.beforeTestMethod(testContext);
		assertEquals(invokedInTx, instance.invoked);
	}

	private void assertBeforeTestMethodWithNonTransactionalTestMethod(Class<? extends Invocable> clazz)
			throws Exception {
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(clazz);
		Invocable instance = clazz.newInstance();
		when(testContext.getTestInstance()).thenReturn(instance);
		when(testContext.getTestMethod()).thenReturn(clazz.getDeclaredMethod("nonTransactionalTest"));

		assertFalse(instance.invoked);
		listener.beforeTestMethod(testContext);
		assertFalse(instance.invoked);
	}

	private void assertAfterTestMethod(Class<? extends Invocable> clazz) throws Exception {
		assertAfterTestMethodWithTransactionalTestMethod(clazz);
		assertAfterTestMethodWithNonTransactionalTestMethod(clazz);
	}

	private void assertAfterTestMethodWithTransactionalTestMethod(Class<? extends Invocable> clazz) throws Exception {
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(clazz);
		Invocable instance = clazz.newInstance();
		when(testContext.getTestInstance()).thenReturn(instance);
		when(testContext.getTestMethod()).thenReturn(clazz.getDeclaredMethod("transactionalTest"));

		when(tm.getTransaction(Mockito.any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());

		assertFalse(instance.invoked);
		listener.beforeTestMethod(testContext);
		listener.afterTestMethod(testContext);
		assertTrue(instance.invoked);
	}

	private void assertAfterTestMethodWithNonTransactionalTestMethod(Class<? extends Invocable> clazz) throws Exception {
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(clazz);
		Invocable instance = clazz.newInstance();
		when(testContext.getTestInstance()).thenReturn(instance);
		when(testContext.getTestMethod()).thenReturn(clazz.getDeclaredMethod("nonTransactionalTest"));

		assertFalse(instance.invoked);
		listener.beforeTestMethod(testContext);
		listener.afterTestMethod(testContext);
		assertFalse(instance.invoked);
	}

	private void assertTransactionConfigurationAttributes(Class<?> clazz, String transactionManagerName,
			boolean defaultRollback) {
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(clazz);

		TransactionConfigurationAttributes attributes = listener.retrieveConfigurationAttributes(testContext);
		assertNotNull(attributes);
		assertEquals(transactionManagerName, attributes.getTransactionManagerName());
		assertEquals(defaultRollback, attributes.isDefaultRollback());
	}

	private void assertIsRollback(Class<?> clazz, boolean rollback) throws NoSuchMethodException, Exception {
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(clazz);
		when(testContext.getTestMethod()).thenReturn(clazz.getDeclaredMethod("test"));
		assertEquals(rollback, listener.isRollback(testContext));
	}

	@Test
	public void beforeTestMethodWithTransactionalDeclaredOnClassLocally() throws Exception {
		assertBeforeTestMethodWithTransactionalTestMethod(TransactionalDeclaredOnClassLocallyTestCase.class);
	}

	@Test
	public void beforeTestMethodWithTransactionalDeclaredOnClassViaMetaAnnotation() throws Exception {
		assertBeforeTestMethodWithTransactionalTestMethod(TransactionalDeclaredOnClassViaMetaAnnotationTestCase.class);
	}

	@Test
	public void beforeTestMethodWithTransactionalDeclaredOnClassViaMetaAnnotationWithOverride() throws Exception {
		// Note: not actually invoked within a transaction since the test class is
		// annotated with @MetaTxWithOverride(propagation = NOT_SUPPORTED)
		assertBeforeTestMethodWithTransactionalTestMethod(
			TransactionalDeclaredOnClassViaMetaAnnotationWithOverrideTestCase.class, false);
	}

	@Test
	public void beforeTestMethodWithTransactionalDeclaredOnMethodViaMetaAnnotationWithOverride() throws Exception {
		// Note: not actually invoked within a transaction since the method is
		// annotated with @MetaTxWithOverride(propagation = NOT_SUPPORTED)
		assertBeforeTestMethodWithTransactionalTestMethod(
			TransactionalDeclaredOnMethodViaMetaAnnotationWithOverrideTestCase.class, false);
		assertBeforeTestMethodWithNonTransactionalTestMethod(TransactionalDeclaredOnMethodViaMetaAnnotationWithOverrideTestCase.class);
	}

	@Test
	public void beforeTestMethodWithTransactionalDeclaredOnMethodLocally() throws Exception {
		assertBeforeTestMethod(TransactionalDeclaredOnMethodLocallyTestCase.class);
	}

	@Test
	public void beforeTestMethodWithTransactionalDeclaredOnMethodViaMetaAnnotation() throws Exception {
		assertBeforeTestMethod(TransactionalDeclaredOnMethodViaMetaAnnotationTestCase.class);
	}

	@Test
	public void beforeTestMethodWithBeforeTransactionDeclaredLocally() throws Exception {
		assertBeforeTestMethod(BeforeTransactionDeclaredLocallyTestCase.class);
	}

	@Test
	public void beforeTestMethodWithBeforeTransactionDeclaredViaMetaAnnotation() throws Exception {
		assertBeforeTestMethod(BeforeTransactionDeclaredViaMetaAnnotationTestCase.class);
	}

	@Test
	public void afterTestMethodWithAfterTransactionDeclaredLocally() throws Exception {
		assertAfterTestMethod(AfterTransactionDeclaredLocallyTestCase.class);
	}

	@Test
	public void afterTestMethodWithAfterTransactionDeclaredViaMetaAnnotation() throws Exception {
		assertAfterTestMethod(AfterTransactionDeclaredViaMetaAnnotationTestCase.class);
	}

	@Test
	public void retrieveConfigurationAttributesWithMissingTransactionConfiguration() throws Exception {
		assertTransactionConfigurationAttributes(MissingTransactionConfigurationTestCase.class, "transactionManager",
			true);
	}

	@Test
	public void retrieveConfigurationAttributesWithEmptyTransactionConfiguration() throws Exception {
		assertTransactionConfigurationAttributes(EmptyTransactionConfigurationTestCase.class, "transactionManager",
			true);
	}

	@Test
	public void retrieveConfigurationAttributesWithExplicitValues() throws Exception {
		assertTransactionConfigurationAttributes(TransactionConfigurationWithExplicitValuesTestCase.class, "tm", false);
	}

	@Test
	public void retrieveConfigurationAttributesViaMetaAnnotation() throws Exception {
		assertTransactionConfigurationAttributes(TransactionConfigurationViaMetaAnnotationTestCase.class, "metaTxMgr",
			true);
	}

	@Test
	public void retrieveConfigurationAttributesViaMetaAnnotationWithOverride() throws Exception {
		assertTransactionConfigurationAttributes(TransactionConfigurationViaMetaAnnotationWithOverrideTestCase.class,
			"overriddenTxMgr", true);
	}

	@Test
	public void isRollbackWithMissingRollback() throws Exception {
		assertIsRollback(MissingRollbackTestCase.class, true);
	}

	@Test
	public void isRollbackWithEmptyRollback() throws Exception {
		assertIsRollback(EmptyRollbackTestCase.class, true);
	}

	@Test
	public void isRollbackWithExplicitValue() throws Exception {
		assertIsRollback(RollbackWithExplicitValueTestCase.class, false);
	}

	@Test
	public void isRollbackViaMetaAnnotation() throws Exception {
		assertIsRollback(RollbackViaMetaAnnotationTestCase.class, false);
	}


	// -------------------------------------------------------------------------

	@Transactional
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface MetaTransactional {
	}

	@Transactional
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface MetaTxWithOverride {

		Propagation propagation() default REQUIRED;
	}

	@BeforeTransaction
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface MetaBeforeTransaction {
	}

	@AfterTransaction
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface MetaAfterTransaction {
	}

	@TransactionConfiguration
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface MetaTxConfig {

		String transactionManager() default "metaTxMgr";
	}

	@Rollback(false)
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface Commit {
	}

	private static abstract class Invocable {

		boolean invoked = false;
	}

	@Transactional
	static class TransactionalDeclaredOnClassLocallyTestCase extends Invocable {

		@BeforeTransaction
		public void beforeTransaction() {
			invoked = true;
		}

		public void transactionalTest() {
			/* no-op */
		}
	}

	static class TransactionalDeclaredOnMethodLocallyTestCase extends Invocable {

		@BeforeTransaction
		public void beforeTransaction() {
			invoked = true;
		}

		@Transactional
		public void transactionalTest() {
			/* no-op */
		}

		public void nonTransactionalTest() {
			/* no-op */
		}
	}

	@MetaTransactional
	static class TransactionalDeclaredOnClassViaMetaAnnotationTestCase extends Invocable {

		@BeforeTransaction
		public void beforeTransaction() {
			invoked = true;
		}

		public void transactionalTest() {
			/* no-op */
		}
	}

	static class TransactionalDeclaredOnMethodViaMetaAnnotationTestCase extends Invocable {

		@BeforeTransaction
		public void beforeTransaction() {
			invoked = true;
		}

		@MetaTransactional
		public void transactionalTest() {
			/* no-op */
		}

		public void nonTransactionalTest() {
			/* no-op */
		}
	}

	@MetaTxWithOverride(propagation = NOT_SUPPORTED)
	static class TransactionalDeclaredOnClassViaMetaAnnotationWithOverrideTestCase extends Invocable {

		@BeforeTransaction
		public void beforeTransaction() {
			invoked = true;
		}

		public void transactionalTest() {
			/* no-op */
		}
	}

	static class TransactionalDeclaredOnMethodViaMetaAnnotationWithOverrideTestCase extends Invocable {

		@BeforeTransaction
		public void beforeTransaction() {
			invoked = true;
		}

		@MetaTxWithOverride(propagation = NOT_SUPPORTED)
		public void transactionalTest() {
			/* no-op */
		}

		public void nonTransactionalTest() {
			/* no-op */
		}
	}

	static class BeforeTransactionDeclaredLocallyTestCase extends Invocable {

		@BeforeTransaction
		public void beforeTransaction() {
			invoked = true;
		}

		@Transactional
		public void transactionalTest() {
			/* no-op */
		}

		public void nonTransactionalTest() {
			/* no-op */
		}
	}

	static class BeforeTransactionDeclaredViaMetaAnnotationTestCase extends Invocable {

		@MetaBeforeTransaction
		public void beforeTransaction() {
			invoked = true;
		}

		@Transactional
		public void transactionalTest() {
			/* no-op */
		}

		public void nonTransactionalTest() {
			/* no-op */
		}
	}

	static class AfterTransactionDeclaredLocallyTestCase extends Invocable {

		@AfterTransaction
		public void afterTransaction() {
			invoked = true;
		}

		@Transactional
		public void transactionalTest() {
			/* no-op */
		}

		public void nonTransactionalTest() {
			/* no-op */
		}
	}

	static class AfterTransactionDeclaredViaMetaAnnotationTestCase extends Invocable {

		@MetaAfterTransaction
		public void afterTransaction() {
			invoked = true;
		}

		@Transactional
		public void transactionalTest() {
			/* no-op */
		}

		public void nonTransactionalTest() {
			/* no-op */
		}
	}

	static class MissingTransactionConfigurationTestCase {
	}

	@TransactionConfiguration
	static class EmptyTransactionConfigurationTestCase {
	}

	@TransactionConfiguration(transactionManager = "tm", defaultRollback = false)
	static class TransactionConfigurationWithExplicitValuesTestCase {
	}

	@MetaTxConfig
	static class TransactionConfigurationViaMetaAnnotationTestCase {
	}

	@MetaTxConfig(transactionManager = "overriddenTxMgr")
	static class TransactionConfigurationViaMetaAnnotationWithOverrideTestCase {
	}

	static class MissingRollbackTestCase {

		public void test() {
		}
	}

	static class EmptyRollbackTestCase {

		@Rollback
		public void test() {
		}
	}

	static class RollbackWithExplicitValueTestCase {

		@Rollback(false)
		public void test() {
		}
	}

	static class RollbackViaMetaAnnotationTestCase {

		@Commit
		public void test() {
		}
	}

}
