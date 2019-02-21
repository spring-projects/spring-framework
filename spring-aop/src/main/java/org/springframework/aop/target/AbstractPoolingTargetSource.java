/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.aop.target;

import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.lang.Nullable;

/**
 * Abstract base class for pooling {@link org.springframework.aop.TargetSource}
 * implementations which maintain a pool of target instances, acquiring and
 * releasing a target object from the pool for each method invocation.
 * This abstract base class is independent of concrete pooling technology;
 * see the subclass {@link CommonsPool2TargetSource} for a concrete example.
 *
 * <p>Subclasses must implement the {@link #getTarget} and
 * {@link #releaseTarget} methods based on their chosen object pool.
 * The {@link #newPrototypeInstance()} method inherited from
 * {@link AbstractPrototypeBasedTargetSource} can be used to create objects
 * in order to put them into the pool.
 *
 * <p>Subclasses must also implement some of the monitoring methods from the
 * {@link PoolingConfig} interface. The {@link #getPoolingConfigMixin()} method
 * makes these stats available on proxied objects through an IntroductionAdvisor.
 *
 * <p>This class implements the {@link org.springframework.beans.factory.DisposableBean}
 * interface in order to force subclasses to implement a {@link #destroy()}
 * method, closing down their object pool.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getTarget
 * @see #releaseTarget
 * @see #destroy
 */
@SuppressWarnings("serial")
public abstract class AbstractPoolingTargetSource extends AbstractPrototypeBasedTargetSource
		implements PoolingConfig, DisposableBean {

	/** The maximum size of the pool. */
	private int maxSize = -1;


	/**
	 * Set the maximum size of the pool.
	 * Default is -1, indicating no size limit.
	 */
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	/**
	 * Return the maximum size of the pool.
	 */
	@Override
	public int getMaxSize() {
		return this.maxSize;
	}


	@Override
	public final void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		super.setBeanFactory(beanFactory);
		try {
			createPool();
		}
		catch (Throwable ex) {
			throw new BeanInitializationException("Could not create instance pool for TargetSource", ex);
		}
	}


	/**
	 * Create the pool.
	 * @throws Exception to avoid placing constraints on pooling APIs
	 */
	protected abstract void createPool() throws Exception;

	/**
	 * Acquire an object from the pool.
	 * @return an object from the pool
	 * @throws Exception we may need to deal with checked exceptions from pool
	 * APIs, so we're forgiving with our exception signature
	 */
	@Override
	@Nullable
	public abstract Object getTarget() throws Exception;

	/**
	 * Return the given object to the pool.
	 * @param target object that must have been acquired from the pool
	 * via a call to {@code getTarget()}
	 * @throws Exception to allow pooling APIs to throw exception
	 * @see #getTarget
	 */
	@Override
	public abstract void releaseTarget(Object target) throws Exception;


	/**
	 * Return an IntroductionAdvisor that providing a mixin
	 * exposing statistics about the pool maintained by this object.
	 */
	public DefaultIntroductionAdvisor getPoolingConfigMixin() {
		DelegatingIntroductionInterceptor dii = new DelegatingIntroductionInterceptor(this);
		return new DefaultIntroductionAdvisor(dii, PoolingConfig.class);
	}

}
