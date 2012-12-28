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

package org.springframework.transaction.support;

import org.springframework.core.Ordered;

/**
 * Simple {@link TransactionSynchronization} adapter containing empty
 * method implementations, for easier overriding of single methods.
 *
 * <p>Also implements the {@link Ordered} interface to enable the execution
 * order of synchronizations to be controlled declaratively. The default
 * {@link #getOrder() order} is {@link Ordered#LOWEST_PRECEDENCE}, indicating
 * late execution; return a lower value for earlier execution.
 *
 * @author Juergen Hoeller
 * @since 22.01.2004
 */
public abstract class TransactionSynchronizationAdapter implements TransactionSynchronization, Ordered {

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	public void suspend() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void flush() {
	}

	@Override
	public void beforeCommit(boolean readOnly) {
	}

	@Override
	public void beforeCompletion() {
	}

	@Override
	public void afterCommit() {
	}

	@Override
	public void afterCompletion(int status) {
	}

}
