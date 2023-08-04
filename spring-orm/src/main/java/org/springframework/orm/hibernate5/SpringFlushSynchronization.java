/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.orm.hibernate5;

import org.hibernate.Session;

import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronization;

/**
 * Simple synchronization adapter that propagates a {@code flush()} call
 * to the underlying Hibernate Session. Used in combination with JTA.
 *
 * @author Juergen Hoeller
 * @since 4.2
 */
public class SpringFlushSynchronization implements TransactionSynchronization {

	private final Session session;


	public SpringFlushSynchronization(Session session) {
		this.session = session;
	}


	@Override
	public void flush() {
		SessionFactoryUtils.flush(this.session, false);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof SpringFlushSynchronization that && this.session == that.session));
	}

	@Override
	public int hashCode() {
		return this.session.hashCode();
	}

}
