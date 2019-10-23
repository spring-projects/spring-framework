/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.tests.aop.advice;

import java.io.IOException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;

import org.springframework.aop.ThrowsAdvice;

@SuppressWarnings("serial")
public class MyThrowsHandler extends MethodCounter implements ThrowsAdvice {
	// Full method signature
	public void afterThrowing(Method m, Object[] args, Object target, IOException ex) {
		count("ioException");
	}
	public void afterThrowing(RemoteException ex) throws Throwable {
		count("remoteException");
	}

	/** Not valid, wrong number of arguments */
	public void afterThrowing(Method m, Exception ex) throws Throwable {
		throw new UnsupportedOperationException("Shouldn't be called");
	}
}
