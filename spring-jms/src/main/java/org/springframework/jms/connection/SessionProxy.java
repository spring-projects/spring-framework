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

package org.springframework.jms.connection;

import javax.jms.Session;

/**
 * Subinterface of {@link javax.jms.Session} to be implemented by
 * Session proxies. Allows access to the the underlying target Session.
 *
 * @author Juergen Hoeller
 * @since 2.0.4
 * @see TransactionAwareConnectionFactoryProxy
 * @see CachingConnectionFactory
 * @see ConnectionFactoryUtils#getTargetSession(javax.jms.Session)
 */
public interface SessionProxy extends Session {

	/**
	 * Return the target Session of this proxy.
	 * <p>This will typically be the native provider Session
	 * or a wrapper from a session pool.
	 * @return the underlying Session (never {@code null})
	 */
	Session getTargetSession();

}
