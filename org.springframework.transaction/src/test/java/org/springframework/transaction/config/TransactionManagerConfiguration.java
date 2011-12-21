/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.transaction.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.CallCountingTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Juergen Hoeller
 */
@Configuration
public class TransactionManagerConfiguration {

	@Bean
	@Qualifier("synch")
	public PlatformTransactionManager transactionManager1() {
		return new CallCountingTransactionManager();
	}

	@Bean
	@Qualifier("noSynch")
	public PlatformTransactionManager transactionManager2() {
		CallCountingTransactionManager tm = new CallCountingTransactionManager();
		tm.setTransactionSynchronization(CallCountingTransactionManager.SYNCHRONIZATION_NEVER);
		return tm;
	}

}
