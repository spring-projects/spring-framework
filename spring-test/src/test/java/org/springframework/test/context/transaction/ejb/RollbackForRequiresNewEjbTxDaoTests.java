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

package org.springframework.test.context.transaction.ejb;

import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

/**
 * Extension of {@link CommitForRequiresNewEjbTxDaoTests} which sets the default
 * rollback semantics for the {@link TransactionalTestExecutionListener} to
 * {@code true}. The transaction managed by the TestContext framework will be
 * rolled back after each test method. Consequently, any work performed in
 * transactional methods that participate in the test-managed transaction will
 * be rolled back automatically. On the other hand, any work performed in
 * transactional methods that do <strong>not</strong> participate in the
 * test-managed transaction will not be affected by the rollback of the
 * test-managed transaction. For example, such work may in fact be committed
 * outside the scope of the test-managed transaction.
 *
 * @author Sam Brannen
 * @since 4.0.1
 */
@Rollback
public class RollbackForRequiresNewEjbTxDaoTests extends CommitForRequiresNewEjbTxDaoTests {

	/* test methods in superclass */

}