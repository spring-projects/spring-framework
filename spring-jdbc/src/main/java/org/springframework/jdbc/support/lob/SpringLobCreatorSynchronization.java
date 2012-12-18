/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.jdbc.support.lob;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.util.Assert;

/**
 * Callback for resource cleanup at the end of a Spring transaction.
 * Invokes {@code LobCreator.close()} to clean up temporary LOBs
 * that might have been created.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see LobCreator#close()
 */
public class SpringLobCreatorSynchronization extends TransactionSynchronizationAdapter {

	/**
	 * Order value for TransactionSynchronization objects that clean up LobCreators.
	 * Return CONNECTION_SYNCHRONIZATION_ORDER - 200 to execute LobCreator cleanup
	 * before Hibernate Session (- 100) and JDBC Connection cleanup, if any.
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#CONNECTION_SYNCHRONIZATION_ORDER
	 */
	public static final int LOB_CREATOR_SYNCHRONIZATION_ORDER =
			DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 200;


	private final LobCreator lobCreator;

	private boolean beforeCompletionCalled = false;


	/**
	 * Create a SpringLobCreatorSynchronization for the given LobCreator.
	 * @param lobCreator the LobCreator to close after transaction completion
	 */
	public SpringLobCreatorSynchronization(LobCreator lobCreator) {
		Assert.notNull(lobCreator, "LobCreator must not be null");
		this.lobCreator = lobCreator;
	}

	@Override
	public int getOrder() {
		return LOB_CREATOR_SYNCHRONIZATION_ORDER;
	}


	@Override
	public void beforeCompletion() {
		// Close the LobCreator early if possible, to avoid issues with strict JTA
		// implementations that issue warnings when doing JDBC operations after
		// transaction completion.
		this.beforeCompletionCalled = true;
		this.lobCreator.close();
	}

	@Override
	public void afterCompletion(int status) {
		if (!this.beforeCompletionCalled) {
			// beforeCompletion not called before (probably because of flushing on commit
			// in the transaction manager, after the chain of beforeCompletion calls).
			// Close the LobCreator here.
			this.lobCreator.close();
		}
	}

}
