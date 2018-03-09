package org.springframework.transaction.support;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Arne Vandamme
 * @since 09.03.2018
 */
public class TransactionTemplateTests {
	@Test
	public void transactionTemplateEquality() {
		TransactionTemplate templateOne = new TransactionTemplate();
		TransactionTemplate templateTwo = new TransactionTemplate();
		assertEquals(templateOne, templateTwo);
		assertEquals(templateOne.hashCode(), templateTwo.hashCode());
		assertEquals(templateOne.toString(), templateTwo.toString());

		AbstractPlatformTransactionManager transactionManagerOne = mock(AbstractPlatformTransactionManager.class);
		templateOne.setTransactionManager(transactionManagerOne);
		templateTwo.setTransactionManager(transactionManagerOne);
		assertEquals(templateOne, templateTwo);
		assertEquals(templateOne.hashCode(), templateTwo.hashCode());
		assertEquals(templateOne.toString(), templateTwo.toString());

		AbstractPlatformTransactionManager transactionManagerTwo = mock(AbstractPlatformTransactionManager.class);
		templateTwo.setTransactionManager(transactionManagerTwo);
		assertNotEquals(templateOne, templateTwo);
		assertNotEquals(templateOne.hashCode(), templateTwo.hashCode());
		assertNotEquals(templateOne.toString(), templateTwo.toString());
	}

}
