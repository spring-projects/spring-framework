/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.transaction.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Very simple implementation of TransactionAttributeSource which will always return
 * the same TransactionAttribute for all methods fed to it. The TransactionAttribute
 * may be specified, but will otherwise default to PROPAGATION_REQUIRED. This may be
 * used in the cases where you want to use the same transaction attribute with all
 * methods being handled by a transaction interceptor.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 15.10.2003
 * @see org.springframework.transaction.interceptor.TransactionProxyFactoryBean
 * @see org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator
 */
@SuppressWarnings("serial")
public class MatchAlwaysTransactionAttributeSource implements TransactionAttributeSource, Serializable {

	private TransactionAttribute transactionAttribute = new DefaultTransactionAttribute();


	/**
	 * Allows a transaction attribute to be specified, using the String form, for
	 * example, "PROPAGATION_REQUIRED".
	 * @param transactionAttribute the String form of the transactionAttribute to use.
	 * @see org.springframework.transaction.interceptor.TransactionAttributeEditor
	 */
	public void setTransactionAttribute(TransactionAttribute transactionAttribute) {
		if (transactionAttribute instanceof DefaultTransactionAttribute dta) {
			dta.resolveAttributeStrings(null);
		}
		this.transactionAttribute = transactionAttribute;
	}


	@Override
	public @Nullable TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
		return (ClassUtils.isUserLevelMethod(method) ? this.transactionAttribute : null);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof MatchAlwaysTransactionAttributeSource that &&
				ObjectUtils.nullSafeEquals(this.transactionAttribute, that.transactionAttribute)));
	}

	@Override
	public int hashCode() {
		return MatchAlwaysTransactionAttributeSource.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.transactionAttribute;
	}

}
