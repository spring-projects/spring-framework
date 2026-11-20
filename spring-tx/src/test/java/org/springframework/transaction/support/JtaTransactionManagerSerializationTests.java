/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.support;

import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import org.junit.jupiter.api.Test;

import org.springframework.context.testfixture.jndi.SimpleNamingContextBuilder;
import org.springframework.core.testfixture.io.SerializationTestUtils;
import org.springframework.transaction.jta.JtaTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Rod Johnson
 */
class JtaTransactionManagerSerializationTests {

	@Test
	void serializable() throws Exception {
		UserTransaction ut1 = mock();
		UserTransaction ut2 = mock();
		TransactionManager tm = mock();

		JtaTransactionManager jtam = new JtaTransactionManager();
		jtam.setUserTransaction(ut1);
		jtam.setTransactionManager(tm);
		jtam.setRollbackOnCommitFailure(true);
		jtam.afterPropertiesSet();

		SimpleNamingContextBuilder jndiEnv = SimpleNamingContextBuilder
				.emptyActivatedContextBuilder();
		jndiEnv.bind(JtaTransactionManager.DEFAULT_USER_TRANSACTION_NAME, ut2);
		JtaTransactionManager serializedJtatm = SerializationTestUtils.serializeAndDeserialize(jtam);

		// should do client-side lookup
		assertThat(serializedJtatm.logger).as("Logger must survive serialization").isNotNull();
		assertThat(serializedJtatm
				.getUserTransaction()).as("UserTransaction looked up on client").isSameAs(ut2);
		assertThat(serializedJtatm
				.getTransactionManager()).as("TransactionManager didn't survive").isNull();
		assertThat(serializedJtatm.isRollbackOnCommitFailure()).isTrue();
	}

}
