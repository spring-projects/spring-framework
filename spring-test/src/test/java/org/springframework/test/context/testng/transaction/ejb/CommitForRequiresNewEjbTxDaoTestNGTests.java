/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.testng.transaction.ejb;

import org.testng.annotations.Test;

import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.transaction.ejb.dao.RequiresNewEjbTxTestEntityDao;

/**
 * Concrete subclass of {@link AbstractEjbTxDaoTestNGTests} which uses the
 * {@link RequiresNewEjbTxTestEntityDao} and sets the default rollback semantics
 * for the {@link TransactionalTestExecutionListener} to {@code false} (i.e.,
 * <em>commit</em>).
 *
 * @author Sam Brannen
 * @since 4.0.1
 */
@Test(suiteName = "Commit for REQUIRES_NEW")
@ContextConfiguration("/org/springframework/test/context/transaction/ejb/requires-new-tx-config.xml")
@Commit
class CommitForRequiresNewEjbTxDaoTestNGTests extends AbstractEjbTxDaoTestNGTests {

	/* test methods in superclass */

}
