/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.transaction.config;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
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
	@Nullable
	public JtaTransactionManager getObject() {
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
