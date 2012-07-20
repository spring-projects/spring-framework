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

package org.springframework.web.context.request.async;

import java.util.concurrent.Callable;

/**
 * A base class for a Callable used to form a chain of Callable instances.
 * Instances of this class are typically registered via
 * {@link AsyncExecutionChain#push(AbstractDelegatingCallable)} in which case
 * there is no need to set the next Callable. Implementations can simply use
 * {@link #getNext()} to delegate to the next Callable and assume it will be set.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 *
 * @see AsyncExecutionChain
 */
public abstract class AbstractDelegatingCallable implements Callable<Object> {

	private Callable<Object> next;

	protected Callable<Object> getNext() {
		return this.next;
	}

	public void setNext(Callable<Object> callable) {
		this.next = callable;
	}

}
