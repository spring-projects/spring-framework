/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.scheduling.support;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.Callable;

import org.springframework.util.Assert;

/**
 * Runnable wrapper that executes call method of its delegate Callable.
 * 
 * @author Serdar Kuzucu
 * @since 4.2.1
 */
public class CallableWrapperRunnable implements Runnable {

	private Callable<?> delegate;

	/**
	 * Create a new CallableWrapperRunnable
	 * 
	 * @param delegate the Callable implementation to delegate to
	 */
	public CallableWrapperRunnable(Callable<?> delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}

	@Override
	public void run() {
		try {
			this.delegate.call();
		}
		catch (Exception e) {
			throw new UndeclaredThrowableException(e);
		}
	}

	@Override
	public String toString() {
		return "CallableWrapperRunnable for " + this.delegate;
	}
}
