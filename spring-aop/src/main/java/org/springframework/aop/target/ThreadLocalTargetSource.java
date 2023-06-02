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

package org.springframework.aop.target;

import java.util.HashSet;
import java.util.Set;

import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.NamedThreadLocal;

/**
 * Alternative to an object pool. This {@link org.springframework.aop.TargetSource}
 * uses a threading model in which every thread has its own copy of the target.
 * There's no contention for targets. Target object creation is kept to a minimum
 * on the running server.
 *
 * <p>Application code is written as to a normal pool; callers can't assume they
 * will be dealing with the same instance in invocations in different threads.
 * However, state can be relied on during the operations of a single thread:
 * for example, if one caller makes repeated calls on the AOP proxy.
 *
 * <p>Cleanup of thread-bound objects is performed on BeanFactory destruction,
 * calling their {@code DisposableBean.destroy()} method if available.
 * Be aware that many thread-bound objects can be around until the application
 * actually shuts down.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see ThreadLocalTargetSourceStats
 * @see org.springframework.beans.factory.DisposableBean#destroy()
 */
@SuppressWarnings("serial")
public class ThreadLocalTargetSource extends AbstractPrototypeBasedTargetSource
		implements ThreadLocalTargetSourceStats, DisposableBean {

	/**
	 * ThreadLocal holding the target associated with the current
	 * thread. Unlike most ThreadLocals, which are static, this variable
	 * is meant to be per thread per instance of the ThreadLocalTargetSource class.
	 */
	private final ThreadLocal<Object> targetInThread =
			new NamedThreadLocal<>("Thread-local instance of bean") {
				@Override
				public String toString() {
					return super.toString() + " '" + getTargetBeanName() + "'";
				}
			};

	/**
	 * Set of managed targets, enabling us to keep track of the targets we've created.
	 */
	private final Set<Object> targetSet = new HashSet<>();

	private int invocationCount;

	private int hitCount;


	/**
	 * Implementation of abstract getTarget() method.
	 * We look for a target held in a ThreadLocal. If we don't find one,
	 * we create one and bind it to the thread. No synchronization is required.
	 */
	@Override
	public Object getTarget() throws BeansException {
		++this.invocationCount;
		Object target = this.targetInThread.get();
		if (target == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("No target for prototype '" + getTargetBeanName() + "' bound to thread: " +
						"creating one and binding it to thread '" + Thread.currentThread().getName() + "'");
			}
			// Associate target with ThreadLocal.
			target = newPrototypeInstance();
			this.targetInThread.set(target);
			synchronized (this.targetSet) {
				this.targetSet.add(target);
			}
		}
		else {
			++this.hitCount;
		}
		return target;
	}

	/**
	 * Dispose of targets if necessary; clear ThreadLocal.
	 * @see #destroyPrototypeInstance
	 */
	@Override
	public void destroy() {
		logger.debug("Destroying ThreadLocalTargetSource bindings");
		synchronized (this.targetSet) {
			for (Object target : this.targetSet) {
				destroyPrototypeInstance(target);
			}
			this.targetSet.clear();
		}
		// Clear ThreadLocal, just in case.
		this.targetInThread.remove();
	}


	@Override
	public int getInvocationCount() {
		return this.invocationCount;
	}

	@Override
	public int getHitCount() {
		return this.hitCount;
	}

	@Override
	public int getObjectCount() {
		synchronized (this.targetSet) {
			return this.targetSet.size();
		}
	}


	/**
	 * Return an introduction advisor mixin that allows the AOP proxy to be
	 * cast to ThreadLocalInvokerStats.
	 */
	public IntroductionAdvisor getStatsMixin() {
		DelegatingIntroductionInterceptor dii = new DelegatingIntroductionInterceptor(this);
		return new DefaultIntroductionAdvisor(dii, ThreadLocalTargetSourceStats.class);
	}

}
