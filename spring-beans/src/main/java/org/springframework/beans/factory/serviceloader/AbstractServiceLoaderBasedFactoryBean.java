/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.serviceloader;

import java.util.ServiceLoader;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Abstract base class for FactoryBeans operating on the
 * JDK 1.6 {@link java.util.ServiceLoader} facility.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see java.util.ServiceLoader
 */
public abstract class AbstractServiceLoaderBasedFactoryBean extends AbstractFactoryBean<Object>
		implements BeanClassLoaderAware {

	private @Nullable Class<?> serviceType;

	private @Nullable ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();


	/**
	 * Specify the desired service type (typically the service's public API).
	 */
	public void setServiceType(@Nullable Class<?> serviceType) {
		this.serviceType = serviceType;
	}

	/**
	 * Return the desired service type.
	 */
	public @Nullable Class<?> getServiceType() {
		return this.serviceType;
	}

	@Override
	public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	/**
	 * Delegates to {@link #getObjectToExpose(java.util.ServiceLoader)}.
	 * @return the object to expose
	 */
	@Override
	protected Object createInstance() {
		Assert.state(getServiceType() != null, "Property 'serviceType' is required");
		return getObjectToExpose(ServiceLoader.load(getServiceType(), this.beanClassLoader));
	}

	/**
	 * Determine the actual object to expose for the given ServiceLoader.
	 * <p>Left to concrete subclasses.
	 * @param serviceLoader the ServiceLoader for the configured service class
	 * @return the object to expose
	 */
	protected abstract Object getObjectToExpose(ServiceLoader<?> serviceLoader);

}
