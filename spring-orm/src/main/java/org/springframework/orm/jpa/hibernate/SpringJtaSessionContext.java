/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.orm.jpa.hibernate;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.context.internal.JTASessionContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Spring-specific subclass of Hibernate's JTASessionContext,
 * setting {@code FlushMode.MANUAL} for read-only transactions.
 *
 * @author Juergen Hoeller
 * @since 7.0
 */
@SuppressWarnings("serial")
class SpringJtaSessionContext extends JTASessionContext {

	public SpringJtaSessionContext(SessionFactoryImplementor factory) {
		super(factory);
	}

	@Override
	protected Session buildOrObtainSession() {
		Session session = super.buildOrObtainSession();
		if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
			session.setHibernateFlushMode(FlushMode.MANUAL);
		}
		return session;
	}

}
