/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.testfixture.CallCountingTransactionManager;

/**
 * @author Juergen Hoeller
 */
@Qualifier("synch")
@SuppressWarnings("serial")
public class SynchTransactionManager extends CallCountingTransactionManager {

}
