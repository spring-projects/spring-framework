/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Adapter that implements the {@link DisposableBean} interface
 * performing various destruction steps on a given bean instance:
 * <ul>
 * <li>DestructionAwareBeanPostProcessors
 * <li>the bean implementing DisposableBean itself
 * <li>a custom destroy method specified on the bean definition
 * </ul>
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see AbstractBeanFactory
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor
 * @see AbstractBeanDefinition#getDestroyMethodName()
 */
class DisposableBeanAdapter implements DisposableBean, Runnable, Serializable {

	private static final Log logger = LogFactory.getLog(DisposableBeanAdapter.class);

	private final Object bean;

	private final String beanName;

	private final boolean invokeDisposableBean;

	private String destroyMethodName;

	private transient Method destroyMethod;

	private List<DestructionAwareBeanPostProcessor> beanPostProcessors;


	/**
	 * Create a new DisposableBeanAdapter for the given bean.
	 * @param bean the bean instance (never <code>null</code>)
	 * @param beanName the name of the bean
	 * @param beanDefinition the merged bean definition
	 * @param postProcessors the List of BeanPostProcessors
	 * (potentially DestructionAwareBeanPostProcessor), if any
	 */
	public DisposableBeanAdapter(Object bean, String beanName, RootBeanDefinition beanDefinition,
			List<BeanPostProcessor> postProcessors) {

		Assert.notNull(bean, "Bean must not be null");
		this.bean = bean;
		this.beanName = beanName;
		this.invokeDisposableBean =
				(this.bean instanceof DisposableBean && !beanDefinition.isExternallyManagedDestroyMethod("destroy"));
		String destroyMethodName = beanDefinition.getDestroyMethodName();
		if (destroyMethodName != null && !(this.invokeDisposableBean && "destroy".equals(destroyMethodName)) &&
				!beanDefinition.isExternallyManagedDestroyMethod(destroyMethodName)) {
			this.destroyMethodName = destroyMethodName;
			try {
				this.destroyMethod = BeanUtils.findMethodWithMinimalParameters(bean.getClass(), destroyMethodName);
			}
			catch (IllegalArgumentException ex) {
				throw new BeanDefinitionValidationException("Couldn't find a unique destroy method on bean with name '" +
						this.beanName + ": " + ex.getMessage());
			}
			if (this.destroyMethod == null) {
				if (beanDefinition.isEnforceDestroyMethod()) {
					throw new BeanDefinitionValidationException("Couldn't find a destroy method named '" +
							destroyMethodName + "' on bean with name '" + beanName + "'");
				}
			}
			else {
				Class[] paramTypes = this.destroyMethod.getParameterTypes();
				if (paramTypes.length > 1) {
					throw new BeanDefinitionValidationException("Method '" + destroyMethodName + "' of bean '" +
							beanName + "' has more than one parameter - not supported as destroy method");
				}
				else if (paramTypes.length == 1 && !paramTypes[0].equals(boolean.class)) {
					throw new BeanDefinitionValidationException("Method '" + destroyMethodName + "' of bean '" +
							beanName + "' has a non-boolean parameter - not supported as destroy method");
				}
			}
		}
		this.beanPostProcessors = filterPostProcessors(postProcessors);
	}

	/**
	 * Create a new DisposableBeanAdapter for the given bean.
	 * @param bean the bean instance (never <code>null</code>)
	 * @param beanName the name of the bean
	 * @param invokeDisposableBean whether to actually invoke DisposableBean's destroy method here
	 * @param destroyMethodName the name of the custom destroy method (<code>null</code> if there is none)
	 * @param postProcessors the List of DestructionAwareBeanPostProcessors, if any
	 */
	private DisposableBeanAdapter(Object bean, String beanName, boolean invokeDisposableBean,
			String destroyMethodName, List<DestructionAwareBeanPostProcessor> postProcessors) {

		this.bean = bean;
		this.beanName = beanName;
		this.invokeDisposableBean = invokeDisposableBean;
		this.destroyMethodName = destroyMethodName;
		this.beanPostProcessors = postProcessors;
	}


	/**
	 * Search for all DestructionAwareBeanPostProcessors in the List.
	 * @param postProcessors the List to search
	 * @return the filtered List of DestructionAwareBeanPostProcessors
	 */
	private List<DestructionAwareBeanPostProcessor> filterPostProcessors(List<BeanPostProcessor> postProcessors) {
		List<DestructionAwareBeanPostProcessor> filteredPostProcessors = null;
		if (postProcessors != null && !postProcessors.isEmpty()) {
			filteredPostProcessors = new ArrayList<DestructionAwareBeanPostProcessor>(postProcessors.size());
			for (BeanPostProcessor postProcessor : postProcessors) {
				if (postProcessor instanceof DestructionAwareBeanPostProcessor) {
					filteredPostProcessors.add((DestructionAwareBeanPostProcessor) postProcessor);
				}
			}
		}
		return filteredPostProcessors;
	}


	public void run() {
		destroy();
	}

	public void destroy() {
		if (this.beanPostProcessors != null && !this.beanPostProcessors.isEmpty()) {
			for (int i = this.beanPostProcessors.size() - 1; i >= 0; i--) {
				this.beanPostProcessors.get(i).postProcessBeforeDestruction(this.bean, this.beanName);
			}
		}

		if (this.invokeDisposableBean) {
			if (logger.isDebugEnabled()) {
				logger.debug("Invoking destroy() on bean with name '" + this.beanName + "'");
			}
			try {
				((DisposableBean) this.bean).destroy();
			}
			catch (Throwable ex) {
				String msg = "Invocation of destroy method failed on bean with name '" + this.beanName + "'";
				if (logger.isDebugEnabled()) {
					logger.warn(msg, ex);
				}
				else {
					logger.warn(msg + ": " + ex);
				}
			}
		}

		if (this.destroyMethod != null) {
			invokeCustomDestroyMethod(this.destroyMethod);
		}
		else if (this.destroyMethodName != null) {
			Method destroyMethod =
					BeanUtils.findMethodWithMinimalParameters(this.bean.getClass(), this.destroyMethodName);
			invokeCustomDestroyMethod(destroyMethod);
		}
	}

	/**
	 * Invoke the specified custom destroy method on the given bean.
	 * <p>This implementation invokes a no-arg method if found, else checking
	 * for a method with a single boolean argument (passing in "true",
	 * assuming a "force" parameter), else logging an error.
	 */
	private void invokeCustomDestroyMethod(Method destroyMethod) {
		Class[] paramTypes = destroyMethod.getParameterTypes();
		Object[] args = new Object[paramTypes.length];
		if (paramTypes.length == 1) {
			args[0] = Boolean.TRUE;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Invoking destroy method '" + this.destroyMethodName +
					"' on bean with name '" + this.beanName + "'");
		}
		ReflectionUtils.makeAccessible(destroyMethod);
		try {
			destroyMethod.invoke(this.bean, args);
		}
		catch (InvocationTargetException ex) {
			String msg = "Invocation of destroy method '" + this.destroyMethodName +
					"' failed on bean with name '" + this.beanName + "'";
			if (logger.isDebugEnabled()) {
				logger.warn(msg, ex.getTargetException());
			}
			else {
				logger.warn(msg + ": " + ex.getTargetException());
			}
		}
		catch (Throwable ex) {
			logger.error("Couldn't invoke destroy method '" + this.destroyMethodName +
					"' on bean with name '" + this.beanName + "'", ex);
		}
	}


	/**
	 * Serializes a copy of the state of this class,
	 * filtering out non-serializable BeanPostProcessors.
	 */
	protected Object writeReplace() {
		List<DestructionAwareBeanPostProcessor> serializablePostProcessors = null;
		if (this.beanPostProcessors != null) {
			serializablePostProcessors = new ArrayList<DestructionAwareBeanPostProcessor>();
			for (DestructionAwareBeanPostProcessor postProcessor : this.beanPostProcessors) {
				if (postProcessor instanceof Serializable) {
					serializablePostProcessors.add(postProcessor);
				}
			}
		}
		return new DisposableBeanAdapter(this.bean, this.beanName, this.invokeDisposableBean,
				this.destroyMethodName, serializablePostProcessors);
	}

}
