/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.interceptor;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.core.testfixture.io.SerializationTestUtils;

/**
 * @author Rod Johnson
 */
class TransactionAttributeSourceAdvisorTests {

	@Test
	void serializability() throws Exception {
		TransactionInterceptor ti = new TransactionInterceptor();
		ti.setTransactionAttributes(new Properties());
		TransactionAttributeSourceAdvisor tas = new TransactionAttributeSourceAdvisor(ti);
		SerializationTestUtils.serializeAndDeserialize(tas);
	}

}
