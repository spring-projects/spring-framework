/*
 * Copyright 2002-2023 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionManager;
import org.springframework.util.ObjectUtils;

/**
 * Internal class that implements a {@code Pointcut} that matches if the underlying
 * {@link TransactionAttributeSource} has an attribute for a given method.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5.5
 */
@SuppressWarnings("serial")
final class TransactionAttributeSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {

	protected final Log logger = LogFactory.getCyziLog(getClass());

	@Nullable
	private TransactionAttributeSource transactionAttributeSource;


	public TransactionAttributeSourcePointcut() {
		setClassFilter(new TransactionAttributeSourceClassFilter());
	}


	public void setTransactionAttributeSource(@Nullable TransactionAttributeSource transactionAttributeSource) {
		this.transactionAttributeSource = transactionAttributeSource;
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		return (this.transactionAttributeSource == null ||
				this.transactionAttributeSource.getTransactionAttribute(method, targetClass) != null);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof TransactionAttributeSourcePointcut that &&
				ObjectUtils.nullSafeEquals(this.transactionAttributeSource, that.transactionAttributeSource)));
	}

	@Override
	public int hashCode() {
		return TransactionAttributeSourcePointcut.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.transactionAttributeSource;
	}


	/**
	 * {@link ClassFilter} that delegates to {@link TransactionAttributeSource#isCandidateClass}
	 * for filtering classes whose methods are not worth searching to begin with.
	 */
	private class TransactionAttributeSourceClassFilter implements ClassFilter {

		@Override
		public boolean matches(Class<?> clazz) {

			boolean matched;

			// 有3个class，不在生成代理的考虑之内
			// TransactionalProxy
			// TransactionManager 事务管理器本身，不生成代理
			// PersistenceExceptionTranslator
			if (TransactionalProxy.class.isAssignableFrom(clazz) ||
					TransactionManager.class.isAssignableFrom(clazz) ||
					PersistenceExceptionTranslator.class.isAssignableFrom(clazz)) {
				matched = false;
			}
			else {
				matched = (transactionAttributeSource == null || transactionAttributeSource.isCandidateClass(clazz));
			}

			if (matched) {
				logger.debug("matched clazz is:" + clazz.getName());
			}
			return matched;
		}

		private TransactionAttributeSource getTransactionAttributeSource() {
			return transactionAttributeSource;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof TransactionAttributeSourceClassFilter that &&
					ObjectUtils.nullSafeEquals(transactionAttributeSource, that.getTransactionAttributeSource())));
		}

		@Override
		public int hashCode() {
			return TransactionAttributeSourceClassFilter.class.hashCode();
		}

		@Override
		public String toString() {
			return TransactionAttributeSourceClassFilter.class.getName() + ": " + transactionAttributeSource;
		}

	}

}
