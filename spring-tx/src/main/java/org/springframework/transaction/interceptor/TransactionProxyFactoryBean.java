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

package org.springframework.transaction.interceptor;

import java.util.Properties;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.AbstractSingletonProxyFactoryBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Proxy factory bean for simplified declarative transaction handling.
 * This is a convenient alternative to a standard AOP
 * {@link org.springframework.aop.framework.ProxyFactoryBean}
 * with a separate {@link TransactionInterceptor} definition.
 *
 * <p><strong>HISTORICAL NOTE:</strong> This class was originally designed to cover the
 * typical case of declarative transaction demarcation: namely, wrapping a singleton
 * target object with a transactional proxy, proxying all the interfaces that the target
 * implements. However, in Spring versions 2.0 and beyond, the functionality provided here
 * is superseded by the more convenient {@code tx:} XML namespace. See the
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/data-access.html#transaction-declarative">declarative transaction management</a>
 * section of the Spring reference documentation to understand modern options for managing
 * transactions in Spring applications. For these reasons, <strong>users should favor
 * the {@code tx:} XML namespace as well as
 * the @{@link org.springframework.transaction.annotation.Transactional Transactional}
 * and @{@link org.springframework.transaction.annotation.EnableTransactionManagement
 * EnableTransactionManagement} annotations.</strong>
 *
 * <p>There are three main properties that need to be specified:
 * <ul>
 * <li>"transactionManager": the {@link PlatformTransactionManager} implementation to use
 * (for example, a {@link org.springframework.transaction.jta.JtaTransactionManager} instance)
 * <li>"target": the target object that a transactional proxy should be created for
 * <li>"transactionAttributes": the transaction attributes (for example, propagation
 * behavior and "readOnly" flag) per target method name (or method name pattern)
 * </ul>
 *
 * <p>If the "transactionManager" property is not set explicitly and this {@link FactoryBean}
 * is running in a {@link ListableBeanFactory}, a single matching bean of type
 * {@link PlatformTransactionManager} will be fetched from the {@link BeanFactory}.
 *
 * <p>In contrast to {@link TransactionInterceptor}, the transaction attributes are
 * specified as properties, with method names as keys and transaction attribute
 * descriptors as values. Method names are always applied to the target class.
 *
 * <p>Internally, a {@link TransactionInterceptor} instance is used, but the user of this
 * class does not have to care. Optionally, a method pointcut can be specified
 * to cause conditional invocation of the underlying {@link TransactionInterceptor}.
 *
 * <p>The "preInterceptors" and "postInterceptors" properties can be set to add
 * additional interceptors to the mix, like
 * {@link org.springframework.aop.interceptor.PerformanceMonitorInterceptor}.
 *
 * <p><b>HINT:</b> This class is often used with parent / child bean definitions.
 * Typically, you will define the transaction manager and default transaction
 * attributes (for method name patterns) in an abstract parent bean definition,
 * deriving concrete child bean definitions for specific target objects.
 * This reduces the per-bean definition effort to a minimum.
 *
 * <pre code="class">
 * &lt;bean id="baseTransactionProxy" class="org.springframework.transaction.interceptor.TransactionProxyFactoryBean"
 *     abstract="true"&gt;
 *   &lt;property name="transactionManager" ref="transactionManager"/&gt;
 *   &lt;property name="transactionAttributes"&gt;
 *     &lt;props&gt;
 *       &lt;prop key="insert*"&gt;PROPAGATION_REQUIRED&lt;/prop&gt;
 *       &lt;prop key="update*"&gt;PROPAGATION_REQUIRED&lt;/prop&gt;
 *       &lt;prop key="*"&gt;PROPAGATION_REQUIRED,readOnly&lt;/prop&gt;
 *     &lt;/props&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="myProxy" parent="baseTransactionProxy"&gt;
 *   &lt;property name="target" ref="myTarget"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="yourProxy" parent="baseTransactionProxy"&gt;
 *   &lt;property name="target" ref="yourTarget"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Juergen Hoeller
 * @author Dmitriy Kopylenko
 * @author Rod Johnson
 * @author Chris Beams
 * @since 21.08.2003
 * @see #setTransactionManager
 * @see #setTarget
 * @see #setTransactionAttributes
 * @see TransactionInterceptor
 * @see org.springframework.aop.framework.ProxyFactoryBean
 */
@SuppressWarnings("serial")
public class TransactionProxyFactoryBean extends AbstractSingletonProxyFactoryBean
		implements BeanFactoryAware {

	private final TransactionInterceptor transactionInterceptor = new TransactionInterceptor();

	@Nullable
	private Pointcut pointcut;


	/**
	 * Set the default transaction manager. This will perform actual
	 * transaction management: This class is just a way of invoking it.
	 * @see TransactionInterceptor#setTransactionManager
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionInterceptor.setTransactionManager(transactionManager);
	}

	/**
	 * Set properties with method names as keys and transaction attribute
	 * descriptors (parsed via TransactionAttributeEditor) as values:
	 * e.g. key = "myMethod", value = "PROPAGATION_REQUIRED,readOnly".
	 * <p>Note: Method names are always applied to the target class,
	 * no matter if defined in an interface or the class itself.
	 * <p>Internally, a NameMatchTransactionAttributeSource will be
	 * created from the given properties.
	 * @see #setTransactionAttributeSource
	 * @see TransactionInterceptor#setTransactionAttributes
	 * @see TransactionAttributeEditor
	 * @see NameMatchTransactionAttributeSource
	 */
	public void setTransactionAttributes(Properties transactionAttributes) {
		this.transactionInterceptor.setTransactionAttributes(transactionAttributes);
	}

	/**
	 * Set the transaction attribute source which is used to find transaction
	 * attributes. If specifying a String property value, a PropertyEditor
	 * will create a MethodMapTransactionAttributeSource from the value.
	 * @see #setTransactionAttributes
	 * @see TransactionInterceptor#setTransactionAttributeSource
	 * @see TransactionAttributeSourceEditor
	 * @see MethodMapTransactionAttributeSource
	 * @see NameMatchTransactionAttributeSource
	 * @see org.springframework.transaction.annotation.AnnotationTransactionAttributeSource
	 */
	public void setTransactionAttributeSource(TransactionAttributeSource transactionAttributeSource) {
		this.transactionInterceptor.setTransactionAttributeSource(transactionAttributeSource);
	}

	/**
	 * Set a pointcut, i.e a bean that can cause conditional invocation
	 * of the TransactionInterceptor depending on method and attributes passed.
	 * Note: Additional interceptors are always invoked.
	 * @see #setPreInterceptors
	 * @see #setPostInterceptors
	 */
	public void setPointcut(Pointcut pointcut) {
		this.pointcut = pointcut;
	}

	/**
	 * This callback is optional: If running in a BeanFactory and no transaction
	 * manager has been set explicitly, a single matching bean of type
	 * {@link PlatformTransactionManager} will be fetched from the BeanFactory.
	 * @see org.springframework.beans.factory.BeanFactory#getBean(Class)
	 * @see org.springframework.transaction.PlatformTransactionManager
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.transactionInterceptor.setBeanFactory(beanFactory);
	}


	/**
	 * Creates an advisor for this FactoryBean's TransactionInterceptor.
	 */
	@Override
	protected Object createMainInterceptor() {
		this.transactionInterceptor.afterPropertiesSet();
		if (this.pointcut != null) {
			return new DefaultPointcutAdvisor(this.pointcut, this.transactionInterceptor);
		}
		else {
			// Rely on default pointcut.
			return new TransactionAttributeSourceAdvisor(this.transactionInterceptor);
		}
	}

	/**
	 * As of 4.2, this method adds {@link TransactionalProxy} to the set of
	 * proxy interfaces in order to avoid re-processing of transaction metadata.
	 */
	@Override
	protected void postProcessProxyFactory(ProxyFactory proxyFactory) {
		proxyFactory.addInterface(TransactionalProxy.class);
	}

}
