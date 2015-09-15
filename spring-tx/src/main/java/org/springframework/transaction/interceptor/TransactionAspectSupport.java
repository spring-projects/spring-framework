/*
 * Copyright 2002-2014 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.core.NamedThreadLocal;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.CallbackPreferringPlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for transactional aspects, such as the {@link TransactionInterceptor}
 * or an AspectJ aspect.
 *
 * <p>This enables the underlying Spring transaction infrastructure to be used easily
 * to implement an aspect for any aspect system.
 *
 * <p>Subclasses are responsible for calling methods in this class in the correct order.
 *
 * <p>If no transaction name has been specified in the {@code TransactionAttribute},
 * the exposed name will be the {@code fully-qualified class name + "." + method name}
 * (by default).
 *
 * <p>Uses the <b>Strategy</b> design pattern. A {@code PlatformTransactionManager}
 * implementation will perform the actual transaction management, and a
 * {@code TransactionAttributeSource} is used for determining transaction definitions.
 *
 * <p>A transaction aspect is serializable if its {@code PlatformTransactionManager}
 * and {@code TransactionAttributeSource} are serializable.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Stéphane Nicoll
 * @author Sam Brannen
 * @since 1.1
 * @see #setTransactionManager
 * @see #setTransactionAttributes
 * @see #setTransactionAttributeSource
 */
public abstract class TransactionAspectSupport implements BeanFactoryAware, InitializingBean {

	/**
	 * Holder to support the {@code currentTransactionStatus()} method,
	 * and to support communication between different cooperating advices
	 * (e.g. before and after advice) if the aspect involves more than a
	 * single method (as will be the case for around advice).
	 */
	private static final ThreadLocal<TransactionInfo> transactionInfoHolder =
			new NamedThreadLocal<TransactionInfo>("Current aspect-driven transaction");

	// NOTE: This class must not implement Serializable because it serves as base
	// class for AspectJ aspects (which are not allowed to implement Serializable)!
	protected final Log logger = LogFactory.getLog(getClass());
	/**
	 * Key to use to store the default transaction manager.
	 */
	private final Object DEFAULT_TRANSACTION_MANAGER_KEY = new Object();
	private final ConcurrentHashMap<Object, PlatformTransactionManager> transactionManagerCache =
			new ConcurrentHashMap<Object, PlatformTransactionManager>();
	/**
	 * Default transaction manager bean name.
	 */
	private String transactionManagerBeanName;
	private TransactionAttributeSource transactionAttributeSource;
	private BeanFactory beanFactory;

	/**
	 * Subclasses can use this to return the current TransactionInfo.
	 * Only subclasses that cannot handle all operations in one method,
	 * such as an AspectJ aspect involving distinct before and after advice,
	 * need to use this mechanism to get at the current TransactionInfo.
	 * An around advice such as an AOP Alliance MethodInterceptor can hold a
	 * reference to the TransactionInfo throughout the aspect method.
	 * <p>A TransactionInfo will be returned even if no transaction was created.
	 * The {@code TransactionInfo.hasTransaction()} method can be used to query this.
	 * <p>To find out about specific transaction characteristics, consider using
	 * TransactionSynchronizationManager's {@code isSynchronizationActive()}
	 * and/or {@code isActualTransactionActive()} methods.
	 *
	 * @return TransactionInfo bound to this thread, or {@code null} if none
	 * @see TransactionInfo#hasTransaction()
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#isSynchronizationActive()
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#isActualTransactionActive()
	 */
	protected static TransactionInfo currentTransactionInfo() throws NoTransactionException {
		return transactionInfoHolder.get();
	}

	/**
	 * Return the transaction status of the current method invocation.
	 * Mainly intended for code that wants to set the current transaction
	 * rollback-only but not throw an application exception.
	 *
	 * @throws NoTransactionException if the transaction info cannot be found,
	 *                                because the method was invoked outside an AOP invocation context
	 */
	public static TransactionStatus currentTransactionStatus() throws NoTransactionException {
		TransactionInfo info = currentTransactionInfo();
		if (info == null || info.transactionStatus == null) {
			throw new NoTransactionException("No transaction aspect-managed TransactionStatus in scope");
		}
		return info.transactionStatus;
	}

	/**
	 * Return the name of the default transaction manager bean.
	 */
	protected final String getTransactionManagerBeanName() {
		return this.transactionManagerBeanName;
	}

	/**
	 * Specify the name of the default transaction manager bean.
	 */
	public void setTransactionManagerBeanName(String transactionManagerBeanName) {
		this.transactionManagerBeanName = transactionManagerBeanName;
	}

	/**
	 * Return the default transaction manager, or {@code null} if unknown.
	 */
	public PlatformTransactionManager getTransactionManager() {
		return this.transactionManagerCache.get(DEFAULT_TRANSACTION_MANAGER_KEY);
	}

	/**
	 * Specify the <em>default</em> transaction manager to use to drive transactions.
	 * <p>The default transaction manager will be used if a <em>qualifier</em>
	 * has not been declared for a given transaction or if an explicit name for the
	 * default transaction manager bean has not been specified.
	 * @see #setTransactionManagerBeanName
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		if (transactionManager != null) {
			this.transactionManagerCache.put(DEFAULT_TRANSACTION_MANAGER_KEY, transactionManager);
		}
	}

	/**
	 * 配置事务属性源
	 * Set properties with method names as keys and transaction attribute
	 * descriptors (parsed via TransactionAttributeEditor) as values:
	 * e.g. key = "myMethod", value = "PROPAGATION_REQUIRED,readOnly".
	 * <p>Note: Method names are always applied to the target class,
	 * no matter if defined in an interface or the class itself.
	 * <p>Internally, a NameMatchTransactionAttributeSource will be
	 * created from the given properties.
	 * @see #setTransactionAttributeSource
	 * @see TransactionAttributeEditor
	 * @see NameMatchTransactionAttributeSource
	 */
	public void setTransactionAttributes(Properties transactionAttributes) {
		// 使用名称匹配的事务属性源
		NameMatchTransactionAttributeSource tas = new NameMatchTransactionAttributeSource();
		tas.setProperties(transactionAttributes);
		this.transactionAttributeSource = tas;
	}

	/**
	 * Set multiple transaction attribute sources which are used to find transaction
	 * attributes. Will build a CompositeTransactionAttributeSource for the given sources.
	 * @see CompositeTransactionAttributeSource
	 * @see MethodMapTransactionAttributeSource
	 * @see NameMatchTransactionAttributeSource
	 * @see org.springframework.transaction.annotation.AnnotationTransactionAttributeSource
	 */
	public void setTransactionAttributeSources(TransactionAttributeSource[] transactionAttributeSources) {
		this.transactionAttributeSource = new CompositeTransactionAttributeSource(transactionAttributeSources);
	}

	/**
	 * Return the transaction attribute source.
	 */
	public TransactionAttributeSource getTransactionAttributeSource() {
		return this.transactionAttributeSource;
	}

	/**
	 * Set the transaction attribute source which is used to find transaction
	 * attributes. If specifying a String property value, a PropertyEditor
	 * will create a MethodMapTransactionAttributeSource from the value.
	 * @see TransactionAttributeSourceEditor
	 * @see MethodMapTransactionAttributeSource
	 * @see NameMatchTransactionAttributeSource
	 * @see org.springframework.transaction.annotation.AnnotationTransactionAttributeSource
	 */
	public void setTransactionAttributeSource(TransactionAttributeSource transactionAttributeSource) {
		this.transactionAttributeSource = transactionAttributeSource;
	}

	/**
	 * Return the BeanFactory to use for retrieving PlatformTransactionManager beans.
	 */
	protected final BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * Set the BeanFactory to use for retrieving PlatformTransactionManager beans.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * Check that required properties were set.
	 */
	@Override
	public void afterPropertiesSet() {
		if (getTransactionManager() == null && this.beanFactory == null) {
			throw new IllegalStateException(
					"Setting the property 'transactionManager' or running in a ListableBeanFactory is required");
		}
		if (this.transactionAttributeSource == null) {
			throw new IllegalStateException(
					"Either 'transactionAttributeSource' or 'transactionAttributes' is required: " +
					"If there are no transactional methods, then don't use a transaction aspect.");
		}
	}


	/**
	 * General delegate for around-advice-based subclasses, delegating to several other template
	 * methods on this class. Able to handle {@link CallbackPreferringPlatformTransactionManager}
	 * as well as regular {@link PlatformTransactionManager} implementations.
	 * @param method the Method being invoked
	 * @param targetClass the target class that we're invoking the method on
	 * @param invocation the callback to use for proceeding with the target invocation
	 * @return the return value of the method, if any
	 * @throws Throwable propagated from the target invocation
	 */
	protected Object invokeWithinTransaction(Method method, Class<?> targetClass, final InvocationCallback invocation)
			throws Throwable {

		// If the transaction attribute is null, the method is non-transactional.
		// 通过事务属性源TransactionAttributeSource读取事务的属性配置，即调用上面名称匹配
		//事务属性源NameMatchTransactionAttributeSource的方法
		final TransactionAttribute txAttr = getTransactionAttributeSource().getTransactionAttribute(method, targetClass);
		// 获取Spring事务管理IoC容器配置的事务处理器
		final PlatformTransactionManager tm = determineTransactionManager(txAttr);
		// 获取目标类指定方法的事务连接点
		final String joinpointIdentification = methodIdentification(method, targetClass);

		//区分不同类型的PlatformTransactionManager事务处理器，不同类型的事务处理器调用
		// 方式不同。对CallbackPreferringPlatformTransactionManager，需要回调函数来
		// 实现事务的创建和提交，对非CallbackPreferringPlatformTransactionManager来//说，则不需要使用回调函数来实现事务处理。
		//非CallbackPreferringPlatformTransactionManager类型的事务处理器
		if (txAttr == null || !(tm instanceof CallbackPreferringPlatformTransactionManager)) {
			// Standard transaction demarcation with getTransaction and commit/rollback calls.
			// 创建事务，将当前事务状态和信息保存到TransactionInfo对象中
			TransactionInfo txInfo = createTransactionIfNecessary(tm, txAttr, joinpointIdentification);
			Object retVal = null;
			try {
				// This is an around advice: Invoke the next interceptor in the chain.
				// This will normally result in a target object being invoked.
				// 沿着拦截器链调用处理，使得最后目标对象的方法得到调用
				retVal = invocation.proceedWithInvocation();
			}
			catch (Throwable ex) {
				// target invocation exception
				// //在调用拦截器拦过程中出现异常，则根据事务配置进行提交或回滚处理
				completeTransactionAfterThrowing(txInfo, ex);
				throw ex;
			}
			finally {
				//清除与当前线程绑定的事务信息
				cleanupTransactionInfo(txInfo);
			}
			//通过事务处理器来对事务进行提交
			commitTransactionAfterReturning(txInfo);
			return retVal;
		}
		// CallbackPreferringPlatformTransactionManager类型的事务处理器
		else {
			// It's a CallbackPreferringPlatformTransactionManager: pass a TransactionCallback in.
			//通过回调函数对事务进行处理
			try {
				//执行实现TransactionCallback接口的doInTransaction回调方法
				Object result = ((CallbackPreferringPlatformTransactionManager) tm).execute(txAttr,
						new TransactionCallback<Object>() {
							//实现TransactionCallback接口匿名内部类的回调方法
							@Override
							public Object doInTransaction(TransactionStatus status) {
								//创建和准备事务
								TransactionInfo txInfo = prepareTransactionInfo(tm, txAttr, joinpointIdentification, status);
								try {
									//沿着拦截器拦调用
									return invocation.proceedWithInvocation();
								}
								//拦截器链处理过程中产生异常
								catch (Throwable ex) {
									//如果事务对异常进行回滚处理
									if (txAttr.rollbackOn(ex)) {
										// A RuntimeException: will lead to a rollback.
										//如果异常时运行时异常，则事务回滚处理
										if (ex instanceof RuntimeException) {
											throw (RuntimeException) ex;
										}
										//如果不是运行时异常，则提交处理
										else {
											throw new ThrowableHolderException(ex);
										}
									}
									//如果事务对异常不进行回滚处理
									else {
										// A normal return value: will lead to a commit.
										//提交处理
										return new ThrowableHolder(ex);
									}
								}
								finally {
									//清除当前线程绑定的事务信息
									cleanupTransactionInfo(txInfo);
								}
							}
						});

				// Check result: It might indicate a Throwable to rethrow.
				//对调用结果异常进行处理。
				//如果是ThrowableHolder类型的异常，则转换为Throwable抛出
				if (result instanceof ThrowableHolder) {
					throw ((ThrowableHolder) result).getThrowable();
				}
				//如果不是ThrowableHolder类型的异常，则异常不做处理直接抛出
				else {
					return result;
				}
			}
			catch (ThrowableHolderException ex) {
				throw ex.getCause();
			}
		}
	}

	/**
	 * Clear the cached transaction managers.
	 */
	protected void clearTransactionManagerCache() {
		this.transactionManagerCache.clear();
	}

	/**
	 * Determine the specific transaction manager to use for the given transaction.
	 */
	protected PlatformTransactionManager determineTransactionManager(TransactionAttribute txAttr) {
		// Do not attempt to lookup tx manager if no tx attributes are set
		if (txAttr == null || this.beanFactory == null) {
			return getTransactionManager();
		}
		String qualifier = txAttr.getQualifier();
		if (StringUtils.hasText(qualifier)) {
			return determineQualifiedTransactionManager(qualifier);
		}
		else if (StringUtils.hasText(this.transactionManagerBeanName)) {
			return determineQualifiedTransactionManager(this.transactionManagerBeanName);
		}
		else {
			PlatformTransactionManager defaultTransactionManager = getTransactionManager();
			if (defaultTransactionManager == null) {
				defaultTransactionManager = this.beanFactory.getBean(PlatformTransactionManager.class);
				this.transactionManagerCache.putIfAbsent(
						DEFAULT_TRANSACTION_MANAGER_KEY, defaultTransactionManager);
			}
			return defaultTransactionManager;
		}
	}

	private PlatformTransactionManager determineQualifiedTransactionManager(String qualifier) {
		PlatformTransactionManager txManager = this.transactionManagerCache.get(qualifier);
		if (txManager == null) {
			txManager = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
					this.beanFactory, PlatformTransactionManager.class, qualifier);
			this.transactionManagerCache.putIfAbsent(qualifier, txManager);
		}
		return txManager;
	}

	/**
	 * Convenience method to return a String representation of this Method
	 * for use in logging. Can be overridden in subclasses to provide a
	 * different identifier for the given method.
	 * @param method the method we're interested in
	 * @param targetClass the class that the method is being invoked on
	 * @return a String representation identifying this method
	 * @see org.springframework.util.ClassUtils#getQualifiedMethodName
	 */
	protected String methodIdentification(Method method, Class<?> targetClass) {
		return (targetClass != null ? targetClass : method.getDeclaringClass()).getName() + "." + method.getName();
	}

	/**
	 * 根据给定的事务属性创建事务对象
	 * Create a transaction if necessary based on the given TransactionAttribute.
	 * <p>Allows callers to perform custom TransactionAttribute lookups through
	 * the TransactionAttributeSource.
	 * @param tm 事务管理器
	 * @param txAttr the TransactionAttribute (may be {@code null})
	 * @param joinpointIdentification the fully qualified method name
	 * (used for monitoring and logging purposes)
	 * @return a TransactionInfo object, whether or not a transaction was created.
	 * The {@code hasTransaction()} method on TransactionInfo can be used to
	 * tell if there was a transaction created.
	 * @see #getTransactionAttributeSource()
	 */
	@SuppressWarnings("serial")
	protected TransactionInfo createTransactionIfNecessary(
			PlatformTransactionManager tm, TransactionAttribute txAttr, final String joinpointIdentification) {

		// If no name specified, apply method identification as transaction name.
		// 读取事务方法调用的事务配置属性
		if (txAttr != null && txAttr.getName() == null) {
			//如果事务名称为null，则使用方法的名称(事务连接点标识)作为事务名称，
			//调用一个实现DelegatingTransactionAttribute接口的匿名内部类
			txAttr = new DelegatingTransactionAttribute(txAttr) {
				//使用方法名称作为事务名称
				@Override
				public String getName() {
					return joinpointIdentification;
				}
			};
		}
		//事务状态封装了事务执行的状态信息
		TransactionStatus status = null;
		if (txAttr != null) {
			if (tm != null) {
				//事务处理器创建事务，并且返回当前事务的状态信息
				status = tm.getTransaction(txAttr);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Skipping transactional joinpoint [" + joinpointIdentification +
							"] because no transaction manager has been configured");
				}
			}
		}
		//准备事务信息，事务信息TransactionInfo封装了事务配置和状态信息
		return prepareTransactionInfo(tm, txAttr, joinpointIdentification, status);
	}

	/**
	 * 准备事务信息
	 * Prepare a TransactionInfo for the given attribute and status object.
	 * @param txAttr the TransactionAttribute (may be {@code null})
	 * @param joinpointIdentification the fully qualified method name
	 * (used for monitoring and logging purposes)
	 * @param status the TransactionStatus for the current transaction
	 * @return the prepared TransactionInfo object
	 */
	protected TransactionInfo prepareTransactionInfo(PlatformTransactionManager tm,
			TransactionAttribute txAttr, String joinpointIdentification, TransactionStatus status) {
//创建事务信息对象
		TransactionInfo txInfo = new TransactionInfo(tm, txAttr, joinpointIdentification);
		//如果事务属性不为null，需要为方法使用事务
		if (txAttr != null) {
			// We need a transaction for this method
			if (logger.isTraceEnabled()) {
				logger.trace("Getting transaction for [" + txInfo.getJoinpointIdentification() + "]");
			}
			//为事务信息对象设置事务状态
			// The transaction manager will flag an error if an incompatible tx already exists
			txInfo.newTransactionStatus(status);
		}
		//如果事务属性为null，不需要为方法使用事务
		else {
			// The TransactionInfo.hasTransaction() method will return
			// false. We created it only to preserve the integrity of
			// the ThreadLocal stack maintained in this class.
			if (logger.isTraceEnabled())
				logger.trace("Don't need to create transaction for [" + joinpointIdentification +
						"]: This method isn't transactional.");
		}

		// We always bind the TransactionInfo to the thread, even if we didn't create
		// a new transaction here. This guarantees that the TransactionInfo stack
		// will be managed correctly even if no transaction was created by this aspect.
		//把当前创建的事务信息对象和线程绑定
		txInfo.bindToThread();
		return txInfo;
	}

	/**
	 * Execute after successful completion of call, but not after an exception was handled.
	 * Do nothing if we didn't create a transaction.
	 * @param txInfo information about the current transaction
	 */
	protected void commitTransactionAfterReturning(TransactionInfo txInfo) {
		if (txInfo != null && txInfo.hasTransaction()) {
			if (logger.isTraceEnabled()) {
				logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() + "]");
			}
			txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
		}
	}

	/**
	 * Handle a throwable, completing the transaction.
	 * We may commit or roll back, depending on the configuration.
	 * @param txInfo information about the current transaction
	 * @param ex throwable encountered
	 */
	protected void completeTransactionAfterThrowing(TransactionInfo txInfo, Throwable ex) {
		if (txInfo != null && txInfo.hasTransaction()) {
			if (logger.isTraceEnabled()) {
				logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() +
						"] after exception: " + ex);
			}
			if (txInfo.transactionAttribute.rollbackOn(ex)) {
				try {
					txInfo.getTransactionManager().rollback(txInfo.getTransactionStatus());
				}
				catch (TransactionSystemException ex2) {
					logger.error("Application exception overridden by rollback exception", ex);
					ex2.initApplicationException(ex);
					throw ex2;
				}
				catch (RuntimeException ex2) {
					logger.error("Application exception overridden by rollback exception", ex);
					throw ex2;
				}
				catch (Error err) {
					logger.error("Application exception overridden by rollback error", ex);
					throw err;
				}
			}
			else {
				// We don't roll back on this exception.
				// Will still roll back if TransactionStatus.isRollbackOnly() is true.
				try {
					txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
				}
				catch (TransactionSystemException ex2) {
					logger.error("Application exception overridden by commit exception", ex);
					ex2.initApplicationException(ex);
					throw ex2;
				}
				catch (RuntimeException ex2) {
					logger.error("Application exception overridden by commit exception", ex);
					throw ex2;
				}
				catch (Error err) {
					logger.error("Application exception overridden by commit error", ex);
					throw err;
				}
			}
		}
	}

	/**
	 * Reset the TransactionInfo ThreadLocal.
	 * <p>Call this in all cases: exception or normal return!
	 * @param txInfo information about the current transaction (may be {@code null})
	 */
	protected void cleanupTransactionInfo(TransactionInfo txInfo) {
		if (txInfo != null) {
			txInfo.restoreThreadLocalStatus();
		}
	}


	/**
	 * Simple callback interface for proceeding with the target invocation.
	 * Concrete interceptors/aspects adapt this to their invocation mechanism.
	 */
	protected interface InvocationCallback {

		Object proceedWithInvocation() throws Throwable;
	}

	/**
	 * Internal holder class for a Throwable, used as a return value
	 * from a TransactionCallback (to be subsequently unwrapped again).
	 */
	private static class ThrowableHolder {

		private final Throwable throwable;

		public ThrowableHolder(Throwable throwable) {
			this.throwable = throwable;
		}

		public final Throwable getThrowable() {
			return this.throwable;
		}
	}

	/**
	 * Internal holder class for a Throwable, used as a RuntimeException to be
	 * thrown from a TransactionCallback (and subsequently unwrapped again).
	 */
	@SuppressWarnings("serial")
	private static class ThrowableHolderException extends RuntimeException {

		public ThrowableHolderException(Throwable throwable) {
			super(throwable);
		}

		@Override
		public String toString() {
			return getCause().toString();
		}
	}

	/**
	 * Opaque object used to hold Transaction information. Subclasses
	 * must pass it back to methods on this class, but not see its internals.
	 */
	protected final class TransactionInfo {

		private final PlatformTransactionManager transactionManager;

		private final TransactionAttribute transactionAttribute;

		private final String joinpointIdentification;

		private TransactionStatus transactionStatus;

		private TransactionInfo oldTransactionInfo;

		public TransactionInfo(PlatformTransactionManager transactionManager,
							   TransactionAttribute transactionAttribute, String joinpointIdentification) {
			this.transactionManager = transactionManager;
			this.transactionAttribute = transactionAttribute;
			this.joinpointIdentification = joinpointIdentification;
		}

		public PlatformTransactionManager getTransactionManager() {
			return this.transactionManager;
		}

		public TransactionAttribute getTransactionAttribute() {
			return this.transactionAttribute;
		}

		/**
		 * Return a String representation of this joinpoint (usually a Method call)
		 * for use in logging.
		 */
		public String getJoinpointIdentification() {
			return this.joinpointIdentification;
		}

		public void newTransactionStatus(TransactionStatus status) {
			this.transactionStatus = status;
		}

		public TransactionStatus getTransactionStatus() {
			return this.transactionStatus;
		}

		/**
		 * Return whether a transaction was created by this aspect,
		 * or whether we just have a placeholder to keep ThreadLocal stack integrity.
		 */
		public boolean hasTransaction() {
			return (this.transactionStatus != null);
		}

		private void bindToThread() {
			// Expose current TransactionStatus, preserving any existing TransactionStatus
			// for restoration after this transaction is complete.
			this.oldTransactionInfo = transactionInfoHolder.get();
			transactionInfoHolder.set(this);
		}

		private void restoreThreadLocalStatus() {
			// Use stack to restore old transaction TransactionInfo.
			// Will be null if none was set.
			transactionInfoHolder.set(this.oldTransactionInfo);
		}

		@Override
		public String toString() {
			return this.transactionAttribute.toString();
		}
	}

}
