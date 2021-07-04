/**
 * AOP-based solution for declarative transaction demarcation.
 * Builds on the AOP infrastructure in org.springframework.aop.framework.
 * Any POJO can be transactionally advised with Spring.
 *
 * <p>The TransactionFactoryProxyBean can be used to create transactional
 * AOP proxies transparently to code that uses them.
 *
 * <p>The TransactionInterceptor is the AOP Alliance MethodInterceptor that
 * delivers transactional advice, based on the Spring transaction abstraction.
 * This allows declarative transaction management in any environment,
 * even without JTA if an application uses only a single database.
 */
@NonNullApi
@NonNullFields
package org.springframework.transaction.interceptor;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
