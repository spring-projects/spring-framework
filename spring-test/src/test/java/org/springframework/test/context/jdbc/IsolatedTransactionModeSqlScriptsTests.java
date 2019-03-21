/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.context.jdbc;

import org.junit.Test;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.SqlConfig.TransactionMode;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;

import static org.junit.Assert.*;

/**
 * Transactional integration tests that verify commit semantics for
 * {@link SqlConfig#transactionMode} and {@link TransactionMode#ISOLATED}.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@ContextConfiguration(classes = PopulatedSchemaDatabaseConfig.class)
@DirtiesContext
public class IsolatedTransactionModeSqlScriptsTests extends AbstractTransactionalJUnit4SpringContextTests {

	@BeforeTransaction
	public void beforeTransaction() {
		assertNumUsers(0);
	}

	@Test
	@SqlGroup(@Sql(scripts = "data-add-dogbert.sql", config = @SqlConfig(transactionMode = TransactionMode.ISOLATED)))
	public void methodLevelScripts() {
		assertNumUsers(1);
	}

	@AfterTransaction
	public void afterTransaction() {
		assertNumUsers(1);
	}

	protected void assertNumUsers(int expected) {
		assertEquals("Number of rows in the 'user' table.", expected, countRowsInTable("user"));
	}

}
