/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.testng.transaction.ejb;

import org.testng.annotations.Test;

import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.transaction.ejb.dao.RequiredEjbTxTestEntityDao;

/**
 * Concrete subclass of {@link AbstractEjbTxDaoTestNGTests} which uses the
 * {@link RequiredEjbTxTestEntityDao} and sets the default rollback semantics
 * for the {@link TransactionalTestExecutionListener} to {@code false} (i.e.,
 * <em>commit</em>).
 *
 * @author Sam Brannen
 * @since 4.0.1
 */
@Test(suiteName = "Commit for REQUIRED")
@ContextConfiguration("/org/springframework/test/context/transaction/ejb/required-tx-config.xml")
@Commit
class CommitForRequiredEjbTxDaoTestNGTests extends AbstractEjbTxDaoTestNGTests {

	/* test methods in superclass */

}
