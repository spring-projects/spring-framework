/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.transaction.ejb;

import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.transaction.ejb.dao.RequiredEjbTxTestEntityDao;

/**
 * Concrete subclass of {@link AbstractEjbTxDaoTests} which uses the
 * {@link RequiredEjbTxTestEntityDao} and sets the default rollback semantics
 * for the {@link TransactionalTestExecutionListener} to {@code false} (i.e.,
 * <em>commit</em>).
 *
 * @author Sam Brannen
 * @since 4.0.1
 */
@ContextConfiguration("required-tx-config.xml")
@Commit
@DisabledInAotMode("@EJB is not supported in Spring AOT")
class CommitForRequiredEjbTxDaoTests extends AbstractEjbTxDaoTests {

	/* test methods in superclass */

}
