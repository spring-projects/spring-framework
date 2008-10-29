/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.transaction.interceptor;

import java.util.Properties;

import org.springframework.util.SerializationTestUtils;

import junit.framework.TestCase;

/**
 * 
 * @author Rod Johnson
 */
public class TransactionAttributeSourceAdvisorTests extends TestCase {
	
	public TransactionAttributeSourceAdvisorTests(String s) {
		super(s);
	}
	
	public void testSerializability() throws Exception {
		TransactionInterceptor ti = new TransactionInterceptor();
		ti.setTransactionAttributes(new Properties());
		TransactionAttributeSourceAdvisor tas = new TransactionAttributeSourceAdvisor(ti);
		SerializationTestUtils.serializeAndDeserialize(tas);
	}

}
