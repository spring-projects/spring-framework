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

package org.springframework.core;

/**
 * Interface to be implemented by transparent resource proxies that need to be
 * considered as equal to the underlying resource, for example for consistent
 * lookup key comparisons. Note that this interface does imply such special
 * semantics and does not constitute a general-purpose mixin!
 *
 * <p>Such wrappers will automatically be unwrapped for key comparisons in
 * {@link org.springframework.transaction.support.TransactionSynchronizationManager}.
 *
 * <p>Only fully transparent proxies, e.g. for redirection or service lookups,
 * are supposed to implement this interface. Proxies that decorate the target
 * object with new behavior, such as AOP proxies, do <i>not</i> qualify here!
 *
 * <p> 透明的资源管理实现接口,需要考虑等同于底层资源,
 * @author Juergen Hoeller
 * @since 2.5.4
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 */
public interface InfrastructureProxy {

	/**
	 * Return the underlying resource (never {@code null}).
	 * 返回底层资源
	 */
	Object getWrappedObject();

}
