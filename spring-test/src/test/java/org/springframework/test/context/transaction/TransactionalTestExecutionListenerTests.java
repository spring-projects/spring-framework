/*
 * Copyright 2002-2015 the original author or authors.
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

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.mockito.BDDMockito;

import org.springframework.core.annotation.AliasFor;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.hamcrest.CoreMatchers.*;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.transaction.annotation.Propagation.*;

/**
 * Unit tests for {@link TransactionalTestExecutionListener}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@SuppressWarnings("deprecation")
public class TransactionalTestExecutionListenerTests {

	private final PlatformTransactionManager tm = mock(PlatformTransactionManager.class);

	private final TransactionalTestExecutionListener listener = new TransactionalTestExecutionListener() {

		protected PlatformTransactionManager getTransactionManager(TestContext testContext, String qualifier) {
			return tm;
		}
	};

	private final TestContext testContext = mock(TestContext.class);

	@Rule
	public ExpectedException exception = ExpectedException.none();


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
		Invocable instance = clazz.newInstance();
		given(testContext.getTestInstance()).willReturn(instance);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("transactionalTest"));

		assertFalse(instance.invoked);
		TransactionContextHolder.removeCurrentTransactionContext();
		listener.beforeTestMethod(testContext);
		assertEquals(invokedInTx, instance.invoked);
	}

	private void assertBeforeTestMethodWithNonTransactionalTestMethod(Class<? extends Invocable> clazz)
			throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		Invocable instance = clazz.newInstance();
		given(testContext.getTestInstance()).willReturn(instance);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("nonTransactionalTest"));

		assertFalse(instance.invoked);
		TransactionContextHolder.removeCurrentTransactionContext();
		listener.beforeTestMethod(testContext);
		assertFalse(instance.invoked);
	}

	private void assertAfterTestMethod(Class<? extends Invocable> clazz) throws Exception {
		assertAfterTestMethodWithTransactionalTestMethod(clazz);
		assertAfterTestMethodWithNonTransactionalTestMethod(clazz);
	}

	private void assertAfterTestMethodWithTransactionalTestMethod(Class<? extends Invocable> clazz) throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		Invocable instance = clazz.newInstance();
		given(testContext.getTestInstance()).willReturn(instance);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("transactionalTest"));

		given(tm.getTransaction(BDDMockito.any(TransactionDefinition.class))).willReturn(new SimpleTransactionStatus());

		assertFalse(instance.invoked);
		TransactionContextHolder.removeCurrentTransactionContext();
		listener.beforeTestMethod(testContext);
		listener.afterTestMethod(testContext);
		assertTrue(instance.invoked);
	}

	private void assertAfterTestMethodWithNonTransactionalTestMethod(Class<? extends Invocable> clazz) throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		Invocable instance = clazz.newInstance();
		given(testContext.getTestInstance()).willReturn(instance);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("nonTransactionalTest"));

		assertFalse(instance.invoked);
		TransactionContextHolder.removeCurrentTransactionContext();
		listener.beforeTestMethod(testContext);
		listener.afterTestMethod(testContext);
		assertFalse(instance.invoked);
	}

	private void assertTransactionConfigurationAttributes(Class<?> clazz, String transactionManagerName,
			boolean defaultRollback) {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);

		TransactionConfigurationAttributes attributes = listener.retrieveConfigurationAttributes(testContext);
		assertNotNull(attributes);
		assertEquals(transactionManagerName, attributes.getTransactionManagerName());
		assertEquals(defaultRollback, attributes.isDefaultRollback());
	}

	private void assertIsRollback(Class<?> clazz, boolean rollback) throws NoSuchMethodException, Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("test"));
		assertEquals(rollback, listener.isRollback(testContext));
	}

	@After
	public void cleanUpThreadLocalStateForSubsequentTestClassesInSuite() {
		TransactionContextHolder.removeCurrentTransactionContext();
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
		assertTransactionConfigurationAttributes(MissingTransactionConfigurationTestCase.class, "", true);
	}

	@Test
	public void retrieveConfigurationAttributesWithEmptyTransactionConfiguration() throws Exception {
		assertTransactionConfigurationAttributes(EmptyTransactionConfigurationTestCase.class, "", true);
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
	public void retrieveConfigurationAttributesWithEmptyTransactionalAnnotation() throws Exception {
		assertTransactionConfigurationAttributes(EmptyTransactionalTestCase.class, "", true);
	}

	@Test
	public void retrieveConfigurationAttributesFromTransactionalAnnotationWithExplicitQualifier() throws Exception {
		// The test class configures "tm" as the qualifier via @Transactional;
		// however, retrieveConfigurationAttributes() only supports
		// @TransactionConfiguration. So we actually expect "" as the qualifier here,
		// relying on beforeTestMethod() to properly obtain the actual qualifier via the
		// TransactionAttribute.
		assertTransactionConfigurationAttributes(TransactionalWithExplicitQualifierTestCase.class, "", true);
	}

	@Test
	public void retrieveConfigurationAttributesFromTransactionalAnnotationViaMetaAnnotation() throws Exception {
		// The test class configures "metaTxMgr" as the qualifier via @Transactional;
		// however, retrieveConfigurationAttributes() only supports
		// @TransactionConfiguration. So we actually expect "" as the qualifier here,
		// relying on beforeTestMethod() to properly obtain the actual qualifier via the
		// TransactionAttribute.
		assertTransactionConfigurationAttributes(TransactionalViaMetaAnnotationTestCase.class, "", true);
	}

	@Test
	public void retrieveConfigurationAttributesFromTransactionalAnnotationViaMetaAnnotationWithExplicitQualifier()
			throws Exception {
		// The test class configures "overriddenTxMgr" as the qualifier via
		// @Transactional; however, retrieveConfigurationAttributes() only supports
		// @TransactionConfiguration. So we actually expect "" as the qualifier here,
		// relying on beforeTestMethod() to properly obtain the actual qualifier via the
		// TransactionAttribute.
		assertTransactionConfigurationAttributes(TransactionalViaMetaAnnotationWithExplicitQualifierTestCase.class, "",
			true);
	}

	@Test
	public void isRollbackWithMissingRollback() throws Exception {
		assertIsRollback(MissingRollbackTestCase.class, true);
	}

	@Test
	public void isRollbackWithEmptyMethodLevelRollback() throws Exception {
		assertIsRollback(EmptyMethodLevelRollbackTestCase.class, true);
	}

	@Test
	public void isRollbackWithMethodLevelRollbackWithExplicitValue() throws Exception {
		assertIsRollback(MethodLevelRollbackWithExplicitValueTestCase.class, false);
	}

	@Test
	public void isRollbackWithMethodLevelRollbackViaMetaAnnotation() throws Exception {
		assertIsRollback(MethodLevelRollbackViaMetaAnnotationTestCase.class, false);
	}

	@Test
	public void isRollbackWithEmptyClassLevelRollback() throws Exception {
		assertIsRollback(EmptyClassLevelRollbackTestCase.class, true);
	}

	@Test
	public void isRollbackWithClassLevelRollbackWithExplicitValue() throws Exception {
		assertIsRollback(ClassLevelRollbackWithExplicitValueTestCase.class, false);
	}

	@Test
	public void isRollbackWithClassLevelRollbackViaMetaAnnotation() throws Exception {
		assertIsRollback(ClassLevelRollbackViaMetaAnnotationTestCase.class, false);
	}

	@Test
	public void isRollbackWithRollbackAndTransactionConfigurationDeclaredAtClassLevel() throws Exception {
		Class<?> clazz = ClassLevelRollbackAndTransactionConfigurationTestCase.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);

		exception.expect(IllegalStateException.class);
		exception.expectMessage(containsString("annotated with both @Rollback and @TransactionConfiguration, but only one is permitted"));
		listener.isRollback(testContext);
	}


	// -------------------------------------------------------------------------

	@Transactional
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface MetaTransactional {
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

	@Transactional
	static class EmptyTransactionalTestCase {
	}

	@Transactional(transactionManager = "tm")
	static class TransactionalWithExplicitQualifierTestCase {
	}

	@MetaTransactional
	static class TransactionalViaMetaAnnotationTestCase {
	}

	@MetaTxWithOverride(transactionManager = "tm")
	static class TransactionalViaMetaAnnotationWithExplicitQualifierTestCase {
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
	@TransactionConfiguration
	static class ClassLevelRollbackAndTransactionConfigurationTestCase {

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

}
