/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.interceptor;

import org.springframework.aop.SpringProxy;

/**
 * A marker interface for manually created transactional proxies.
 *
 * <p>{@link TransactionAttributeSourcePointcut} will ignore such existing
 * transactional proxies during AOP auto-proxying and therefore avoid
 * re-processing transaction metadata on them.
 *
 * @author Juergen Hoeller
 * @since 4.1.7
 */
public interface TransactionalProxy extends SpringProxy {

}
