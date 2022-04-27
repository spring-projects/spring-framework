/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.lang.reflect.Method;

/**
 * Interface to be implemented by classes that can reimplement any method on an
 * IoC-managed object: the <b>Method Injection</b> form of Dependency Injection.
 *
 * <p>Such methods may be (but need not be) abstract, in which case the
 * container will create a concrete subclass to instantiate.
 *
 * @author Rod Johnson
 * @since 1.1
 */
public interface MethodReplacer {

	/**
	 * Reimplement the given method.
	 * @param obj the instance we're reimplementing the method for
	 * @param method the method to reimplement
	 * @param args arguments to the method
	 * @return return value for the method
	 */
	Object reimplement(Object obj, Method method, Object[] args) throws Throwable;

}
