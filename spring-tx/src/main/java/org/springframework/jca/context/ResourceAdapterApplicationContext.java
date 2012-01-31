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

package org.springframework.jca.context;

import javax.resource.spi.BootstrapContext;
import javax.resource.spi.work.WorkManager;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.context.ApplicationContext} implementation
 * for a JCA ResourceAdapter. Needs to be initialized with the JCA
 * {@link javax.resource.spi.BootstrapContext}, passing it on to
 * Spring-managed beans that implement {@link BootstrapContextAware}.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see SpringContextResourceAdapter
 * @see BootstrapContextAware
 */
public class ResourceAdapterApplicationContext extends GenericApplicationContext {

	private final BootstrapContext bootstrapContext;


	/**
	 * Create a new ResourceAdapterApplicationContext for the given BootstrapContext.
	 * @param bootstrapContext the JCA BootstrapContext that the ResourceAdapter
	 * has been started with
	 */
	public ResourceAdapterApplicationContext(BootstrapContext bootstrapContext) {
		Assert.notNull(bootstrapContext, "BootstrapContext must not be null");
		this.bootstrapContext = bootstrapContext;
	}


	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		beanFactory.addBeanPostProcessor(new BootstrapContextAwareProcessor(this.bootstrapContext));
		beanFactory.ignoreDependencyInterface(BootstrapContextAware.class);
		beanFactory.registerResolvableDependency(BootstrapContext.class, this.bootstrapContext);

		// JCA WorkManager resolved lazily - may not be available.
		beanFactory.registerResolvableDependency(WorkManager.class, new ObjectFactory<WorkManager>() {
			public WorkManager getObject() {
				return bootstrapContext.getWorkManager();
			}
		});
	}

}
