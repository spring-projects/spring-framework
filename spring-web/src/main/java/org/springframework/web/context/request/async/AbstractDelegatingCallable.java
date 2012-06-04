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
 * A base class for a Callable that can be used in a chain of Callable instances.
 *
 * <p>Typical use for async request processing scenarios involves:
 * <ul>
 * 	<li>Create an instance of this type and register it via
 * {@link AsyncExecutionChain#addDelegatingCallable(AbstractDelegatingCallable)}
 * (internally the nodes of the chain will be linked so no need to set up "next").
 * 	<li>Provide an implementation of {@link Callable#call()} that contains the
 * logic needed to complete request processing outside the main processing thread.
 * 	<li>In the implementation, delegate to the next Callable to obtain
 * its result, e.g. ModelAndView, and then do some post-processing, e.g. view
 * resolution. In some cases both pre- and post-processing might be
 * appropriate, e.g. setting up {@link ThreadLocal} storage.
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 *
 * @see AsyncExecutionChain
 */
public abstract class AbstractDelegatingCallable implements Callable<Object> {

	private Callable<Object> next;

	public void setNextCallable(Callable<Object> nextCallable) {
		this.next = nextCallable;
	}

	protected Callable<Object> getNextCallable() {
		return this.next;
	}

}
