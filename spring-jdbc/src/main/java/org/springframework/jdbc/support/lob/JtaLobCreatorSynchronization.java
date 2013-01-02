/*
 * Copyright 2002-2012 the original author or authors.
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

import javax.transaction.Synchronization;

import org.springframework.util.Assert;

/**
 * Callback for resource cleanup at the end of a JTA transaction.
 * Invokes {@code LobCreator.close()} to clean up temporary LOBs
 * that might have been created.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see LobCreator#close()
 * @see javax.transaction.Transaction#registerSynchronization
 */
public class JtaLobCreatorSynchronization implements Synchronization {

	private final LobCreator lobCreator;

	private boolean beforeCompletionCalled = false;


	/**
	 * Create a JtaLobCreatorSynchronization for the given LobCreator.
	 * @param lobCreator the LobCreator to close after transaction completion
	 */
	public JtaLobCreatorSynchronization(LobCreator lobCreator) {
		Assert.notNull(lobCreator, "LobCreator must not be null");
		this.lobCreator = lobCreator;
	}

	public void beforeCompletion() {
		// Close the LobCreator early if possible, to avoid issues with strict JTA
		// implementations that issue warnings when doing JDBC operations after
		// transaction completion.
		this.beforeCompletionCalled = true;
		this.lobCreator.close();
	}

	public void afterCompletion(int status) {
		if (!this.beforeCompletionCalled) {
			// beforeCompletion not called before (probably because of JTA rollback).
			// Close the LobCreator here.
			this.lobCreator.close();
		}
	}

}
