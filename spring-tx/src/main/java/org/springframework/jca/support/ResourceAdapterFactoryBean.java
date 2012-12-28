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

package org.springframework.jca.support;

import javax.resource.ResourceException;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.WorkManager;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.FactoryBean} that bootstraps
 * the specified JCA 1.5 {@link javax.resource.spi.ResourceAdapter},
 * starting it with a local {@link javax.resource.spi.BootstrapContext}
 * and exposing it for bean references. It will also stop the ResourceAdapter
 * on context shutdown. This corresponds to 'non-managed' bootstrap in a
 * local environment, according to the JCA 1.5 specification.
 *
 * <p>This is essentially an adapter for bean-style bootstrapping of a
 * JCA ResourceAdapter, allowing the BootstrapContext or its elements
 * (such as the JCA WorkManager) to be specified through bean properties.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see #setResourceAdapter
 * @see #setBootstrapContext
 * @see #setWorkManager
 * @see javax.resource.spi.ResourceAdapter#start(javax.resource.spi.BootstrapContext)
 * @see javax.resource.spi.ResourceAdapter#stop()
 */
public class ResourceAdapterFactoryBean implements FactoryBean<ResourceAdapter>, InitializingBean, DisposableBean {

	private ResourceAdapter resourceAdapter;

	private BootstrapContext bootstrapContext;

	private WorkManager workManager;

	private XATerminator xaTerminator;


	/**
	 * Specify the target JCA ResourceAdapter as class, to be instantiated
	 * with its default configuration.
	 * <p>Alternatively, specify a pre-configured ResourceAdapter instance
	 * through the "resourceAdapter" property.
	 * @see #setResourceAdapter
	 */
	public void setResourceAdapterClass(Class resourceAdapterClass) {
		Assert.isAssignable(ResourceAdapter.class, resourceAdapterClass);
		this.resourceAdapter = (ResourceAdapter) BeanUtils.instantiateClass(resourceAdapterClass);
	}

	/**
	 * Specify the target JCA ResourceAdapter, passed in as configured instance
	 * which hasn't been started yet. This will typically happen as an
	 * inner bean definition, configuring the ResourceAdapter instance
	 * through its vendor-specific bean properties.
	 */
	public void setResourceAdapter(ResourceAdapter resourceAdapter) {
		this.resourceAdapter = resourceAdapter;
	}

	/**
	 * Specify the JCA BootstrapContext to use for starting the ResourceAdapter.
	 * <p>Alternatively, you can specify the individual parts (such as the
	 * JCA WorkManager) as individual references.
	 * @see #setWorkManager
	 * @see #setXaTerminator
	 */
	public void setBootstrapContext(BootstrapContext bootstrapContext) {
		this.bootstrapContext = bootstrapContext;
	}

	/**
	 * Specify the JCA WorkManager to use for bootstrapping the ResourceAdapter.
	 * @see #setBootstrapContext
	 */
	public void setWorkManager(WorkManager workManager) {
		this.workManager = workManager;
	}

	/**
	 * Specify the JCA XATerminator to use for bootstrapping the ResourceAdapter.
	 * @see #setBootstrapContext
	 */
	public void setXaTerminator(XATerminator xaTerminator) {
		this.xaTerminator = xaTerminator;
	}


	/**
	 * Builds the BootstrapContext and starts the ResourceAdapter with it.
	 * @see javax.resource.spi.ResourceAdapter#start(javax.resource.spi.BootstrapContext)
	 */
	@Override
	public void afterPropertiesSet() throws ResourceException {
		if (this.resourceAdapter == null) {
			throw new IllegalArgumentException("'resourceAdapter' or 'resourceAdapterClass' is required");
		}
		if (this.bootstrapContext == null) {
			this.bootstrapContext = new SimpleBootstrapContext(this.workManager, this.xaTerminator);
		}
		this.resourceAdapter.start(this.bootstrapContext);
	}


	@Override
	public ResourceAdapter getObject() {
		return this.resourceAdapter;
	}

	@Override
	public Class<? extends ResourceAdapter> getObjectType() {
		return (this.resourceAdapter != null ? this.resourceAdapter.getClass() : ResourceAdapter.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * Stops the ResourceAdapter.
	 * @see javax.resource.spi.ResourceAdapter#stop()
	 */
	@Override
	public void destroy() {
		this.resourceAdapter.stop();
	}

}
