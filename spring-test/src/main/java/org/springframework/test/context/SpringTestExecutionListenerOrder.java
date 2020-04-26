/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.context;

/**
 * Aggregates the orders of the {@link TestExecutionListener}s
 * defined by the Spring TestContext Framework.
 *
 * @author Andrei Dragnea
 * @since 5.2.6
 * @see org.springframework.core.Ordered
 * @see TestExecutionListener
 */
public enum SpringTestExecutionListenerOrder {

	/**
	 * Order of {@link org.springframework.test.context.web.ServletTestExecutionListener}.
	 */
	SERVLET(1_000),

	/**
	 * Order of {@link org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener}.
	 */
	DIRTIES_CONTEXT_BEFORE_MODES(1_500),

	/**
	 * Order of {@link org.springframework.test.context.support.DependencyInjectionTestExecutionListener}.
	 */
	DEPENDENCY_INJECTION(2_000),

	/**
	 * Order of {@link org.springframework.test.context.support.DirtiesContextTestExecutionListener}.
	 */
	DIRTIES_CONTEXT(3_000),

	/**
	 * Order of {@link org.springframework.test.context.transaction.TransactionalTestExecutionListener}.
	 */
	TRANSACTIONAL(4_000),

	/**
	 * Order of {@link org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener}.
	 */
	SQL_SCRIPTS(5_000),

	/**
	 * Order of {@link org.springframework.test.context.event.EventPublishingTestExecutionListener}.
	 */
	EVENT_PUBLISHING(10_000);

	private final int value;

	SpringTestExecutionListenerOrder(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

}
