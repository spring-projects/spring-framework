/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.transaction;

import javax.transaction.xa.XAResource;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;

/**
 * @author Juergen Hoeller
 * @since 31.08.2004
 */
class MockJtaTransaction implements jakarta.transaction.Transaction {

	private Synchronization synchronization;

	@Override
	public int getStatus() {
		return Status.STATUS_ACTIVE;
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
		this.synchronization = synchronization;
	}

	public Synchronization getSynchronization() {
		return synchronization;
	}

	@Override
	public boolean enlistResource(XAResource xaResource) {
		return false;
	}

	@Override
	public boolean delistResource(XAResource xaResource, int i) {
		return false;
	}

	@Override
	public void commit() {
	}

	@Override
	public void rollback() {
	}

	@Override
	public void setRollbackOnly() {
	}

}
