/*
 * Copyright 2002-present the original author or authors.
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
