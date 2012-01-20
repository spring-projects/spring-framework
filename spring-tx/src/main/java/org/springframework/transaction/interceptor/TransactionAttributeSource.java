/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.transaction.interceptor;

import java.lang.reflect.Method;

/**
 * Interface used by TransactionInterceptor. Implementations know
 * how to source transaction attributes, whether from configuration,
 * metadata attributes at source level, or anywhere else.
 *
 * @author Rod Johnson
 * @since 15.04.2003
 * @see TransactionInterceptor#setTransactionAttributeSource
 * @see TransactionProxyFactoryBean#setTransactionAttributeSource
 */
public interface TransactionAttributeSource {

	/**
	 * Return the transaction attribute for this method.
	 * Return null if the method is non-transactional.
	 * @param method method
	 * @param targetClass target class. May be <code>null</code>, in which
	 * case the declaring class of the method must be used.
	 * @return TransactionAttribute the matching transaction attribute,
	 * or <code>null</code> if none found
	 */
	TransactionAttribute getTransactionAttribute(Method method, Class<?> targetClass);

}
