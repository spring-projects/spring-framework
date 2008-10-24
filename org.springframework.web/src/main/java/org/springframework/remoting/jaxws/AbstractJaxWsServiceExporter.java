/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.remoting.jaxws;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ConcurrentExecutorAdapter;

/**
 * Abstract exporter for JAX-WS services, autodetecting annotated service beans
 * (through the JAX-WS {@link javax.jws.WebService} annotation). Subclasses
 * need to implement the {@link #publishEndpoint} template method for actual
 * endpoint exposure.
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 * @see javax.jws.WebService
 * @see javax.xml.ws.Endpoint
 * @see SimpleJaxWsServiceExporter
 * @see SimpleHttpServerJaxWsServiceExporter
 */
public abstract class AbstractJaxWsServiceExporter implements BeanFactoryAware, InitializingBean, DisposableBean {

	private Map<String, Object> endpointProperties;

	private Executor executor;

	private ListableBeanFactory beanFactory;

	private final Set<Endpoint> publishedEndpoints = new LinkedHashSet<Endpoint>();


	/**
	 * Set the property bag for the endpoint, including properties such as
	 * "javax.xml.ws.wsdl.service" or "javax.xml.ws.wsdl.port".
	 * @see javax.xml.ws.Endpoint#setProperties
	 * @see javax.xml.ws.Endpoint#WSDL_SERVICE
	 * @see javax.xml.ws.Endpoint#WSDL_PORT
	 */
	public void setEndpointProperties(Map<String, Object> endpointProperties) {
		this.endpointProperties = endpointProperties;
	}

	/**
	 * Set the JDK concurrent executor to use for dispatching incoming requests
	 * to exported service instances.
	 * @see javax.xml.ws.Endpoint#setExecutor
	 */
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	/**
	 * Set the Spring TaskExecutor to use for dispatching incoming requests
	 * to exported service instances.
	 * @see javax.xml.ws.Endpoint#setExecutor
	 */
	public void setTaskExecutor(TaskExecutor executor) {
		this.executor = new ConcurrentExecutorAdapter(executor);
	}

	/**
	 * Obtains all web service beans and publishes them as JAX-WS endpoints.
	 */
	public void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ListableBeanFactory)) {
			throw new IllegalStateException(getClass().getSimpleName() + " requires a ListableBeanFactory");
		}
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}


	/**
	 * Immediately publish all endpoints when fully configured.
	 * @see #publishEndpoints()
	 */
	public void afterPropertiesSet() throws Exception {
		publishEndpoints();
	}

	/**
	 * Publish all {@link javax.jws.WebService} annotated beans in the
	 * containing BeanFactory.
	 * @see #publishEndpoint
	 */
	public void publishEndpoints() {
		String[] beanNames = this.beanFactory.getBeanNamesForType(Object.class, false, false);
		for (String beanName : beanNames) {
			Class<?> type = this.beanFactory.getType(beanName);
			WebService annotation = type.getAnnotation(WebService.class);
			if (annotation != null) {
				Endpoint endpoint = Endpoint.create(this.beanFactory.getBean(beanName));
				if (this.endpointProperties != null) {
					endpoint.setProperties(this.endpointProperties);
				}
				if (this.executor != null) {
					endpoint.setExecutor(this.executor);
				}
				publishEndpoint(endpoint, annotation);
				this.publishedEndpoints.add(endpoint);
			}
		}
	}

	/**
	 * Actually publish the given endpoint. To be implemented by subclasses.
	 * @param endpoint the JAX-WS Endpoint object
	 * @param annotation the service bean's WebService annotation
	 */
	protected abstract void publishEndpoint(Endpoint endpoint, WebService annotation);


	/**
	 * Stops all published endpoints, taking the web services offline.
	 */
	public void destroy() {
		for (Endpoint endpoint : this.publishedEndpoints) {
			endpoint.stop();
		}
	}

}
