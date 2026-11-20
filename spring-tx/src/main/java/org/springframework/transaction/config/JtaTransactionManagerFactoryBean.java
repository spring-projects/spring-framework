/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.config;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.jta.JtaTransactionManager;

/**
 * A {@link FactoryBean} equivalent to the &lt;tx:jta-transaction-manager/&gt; XML element.
 *
 * @author Juergen Hoeller
 * @since 4.1.1
 * @deprecated as of 6.0, in favor of a straight {@link JtaTransactionManager} definition
 */
@Deprecated(since = "6.0")
public class JtaTransactionManagerFactoryBean implements FactoryBean<JtaTransactionManager>, InitializingBean {

	private final JtaTransactionManager transactionManager = new JtaTransactionManager();


	@Override
	public void afterPropertiesSet() throws TransactionSystemException {
		this.transactionManager.afterPropertiesSet();
	}

	@Override
	public @Nullable JtaTransactionManager getObject() {
		return this.transactionManager;
	}

	@Override
	public Class<?> getObjectType() {
		return this.transactionManager.getClass();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
