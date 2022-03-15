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

package org.springframework.transaction.interceptor;

import io.micrometer.core.instrument.observation.ObservationRegistry;

import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.testfixture.CallCountingTransactionManager;
import org.springframework.transaction.testfixture.ObservationTransactionManagerSampleTestRunner;

import static org.assertj.core.api.BDDAssertions.then;

/**
 *
 * @author Marcin Grzejszczak
 * @since 6.0.0
 */
class ObservationTransactionTemplateTests extends ObservationTransactionManagerSampleTestRunner<TransactionTemplate> {

	@Override
	protected TransactionTemplate given(ObservationRegistry observationRegistry) {
		TransactionTemplate transactionTemplate = new TransactionTemplate();
		transactionTemplate.setTransactionManager(new CallCountingTransactionManager());
		transactionTemplate.setObservationRegistry(observationRegistry);
		return transactionTemplate;
	}

	@Override
	protected void when(TransactionTemplate sut) {
		then(sut.execute((TransactionCallback<Object>) status -> "hello")).isEqualTo("hello");
	}

}
