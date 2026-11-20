/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.serviceloader;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanClassLoaderAware;

/**
 * {@link org.springframework.beans.factory.FactoryBean} that exposes the
 * 'primary' service for the configured service class, obtained through
 * the JDK 1.6 {@link java.util.ServiceLoader} facility.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see java.util.ServiceLoader
 */
public class ServiceFactoryBean extends AbstractServiceLoaderBasedFactoryBean implements BeanClassLoaderAware {

	@Override
	protected Object getObjectToExpose(ServiceLoader<?> serviceLoader) {
		Iterator<?> it = serviceLoader.iterator();
		if (!it.hasNext()) {
			throw new IllegalStateException(
					"ServiceLoader could not find service for type [" + getServiceType() + "]");
		}
		return it.next();
	}

	@Override
	public @Nullable Class<?> getObjectType() {
		return getServiceType();
	}

}
