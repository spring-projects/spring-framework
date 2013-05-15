/*
 * Copyright 2002-2012 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.WebServiceProvider;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Abstract exporter for JAX-WS services, autodetecting annotated service beans
 * (through the JAX-WS {@link javax.jws.WebService} annotation).
 * Compatible with JAX-WS 2.0, 2.1 and 2.2.
 *
 * <p>Subclasses need to implement the {@link #publishEndpoint} template methods
 * for actual endpoint exposure.
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

	private String bindingType;

	private Object[] webServiceFeatures;

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
	 * Specify the binding type to use, overriding the value of
	 * the JAX-WS {@link javax.xml.ws.BindingType} annotation.
	 */
	public void setBindingType(String bindingType) {
		this.bindingType = bindingType;
	}

	/**
	 * Allows for providing JAX-WS 2.2 WebServiceFeature specifications:
	 * in the form of actual {@link javax.xml.ws.WebServiceFeature} objects,
	 * WebServiceFeature Class references, or WebServiceFeature class names.
	 */
	public void setWebServiceFeatures(Object[] webServiceFeatures) {
		this.webServiceFeatures = webServiceFeatures;
	}

	/**
	 * Obtains all web service beans and publishes them as JAX-WS endpoints.
	 */
	@Override
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
	@Override
	public void afterPropertiesSet() throws Exception {
		publishEndpoints();
	}

	/**
	 * Publish all {@link javax.jws.WebService} annotated beans in the
	 * containing BeanFactory.
	 * @see #publishEndpoint
	 */
	public void publishEndpoints() {
		Set<String> beanNames = new LinkedHashSet<String>(this.beanFactory.getBeanDefinitionCount());
		beanNames.addAll(Arrays.asList(this.beanFactory.getBeanDefinitionNames()));
		if (this.beanFactory instanceof ConfigurableBeanFactory) {
			beanNames.addAll(Arrays.asList(((ConfigurableBeanFactory) this.beanFactory).getSingletonNames()));
		}
		for (String beanName : beanNames) {
			try {
				Class<?> type = this.beanFactory.getType(beanName);
				if (type != null && !type.isInterface()) {
					WebService wsAnnotation = type.getAnnotation(WebService.class);
					WebServiceProvider wsProviderAnnotation = type.getAnnotation(WebServiceProvider.class);
					if (wsAnnotation != null || wsProviderAnnotation != null) {
						Endpoint endpoint = createEndpoint(this.beanFactory.getBean(beanName));
						if (this.endpointProperties != null) {
							endpoint.setProperties(this.endpointProperties);
						}
						if (this.executor != null) {
							endpoint.setExecutor(this.executor);
						}
						if (wsAnnotation != null) {
							publishEndpoint(endpoint, wsAnnotation);
						}
						else {
							publishEndpoint(endpoint, wsProviderAnnotation);
						}
						this.publishedEndpoints.add(endpoint);
					}
				}
			}
			catch (CannotLoadBeanClassException ex) {
				// ignore beans where the class is not resolvable
			}
		}
	}

	/**
	 * Create the actual Endpoint instance.
	 * @param bean the service object to wrap
	 * @return the Endpoint instance
	 * @see Endpoint#create(Object)
	 * @see Endpoint#create(String, Object)
	 */
	protected Endpoint createEndpoint(Object bean) {
		if (this.webServiceFeatures != null) {
			return new FeatureEndpointProvider().createEndpoint(this.bindingType, bean, this.webServiceFeatures);
		}
		else {
			return Endpoint.create(this.bindingType, bean);
		}
	}


	/**
	 * Actually publish the given endpoint. To be implemented by subclasses.
	 * @param endpoint the JAX-WS Endpoint object
	 * @param annotation the service bean's WebService annotation
	 */
	protected abstract void publishEndpoint(Endpoint endpoint, WebService annotation);

	/**
	 * Actually publish the given provider endpoint. To be implemented by subclasses.
	 * @param endpoint the JAX-WS Provider Endpoint object
	 * @param annotation the service bean's WebServiceProvider annotation
	 */
	protected abstract void publishEndpoint(Endpoint endpoint, WebServiceProvider annotation);


	/**
	 * Stops all published endpoints, taking the web services offline.
	 */
	@Override
	public void destroy() {
		for (Endpoint endpoint : this.publishedEndpoints) {
			endpoint.stop();
		}
	}


	/**
	 * Inner class in order to avoid a hard-coded JAX-WS 2.2 dependency.
	 * JAX-WS 2.0 and 2.1 didn't have WebServiceFeatures for endpoints yet...
	 */
	private class FeatureEndpointProvider {

		public Endpoint createEndpoint(String bindingType, Object implementor, Object[] features) {
			WebServiceFeature[] wsFeatures = new WebServiceFeature[features.length];
			for (int i = 0; i < features.length; i++) {
				wsFeatures[i] = convertWebServiceFeature(features[i]);
			}
			try {
				Method create = Endpoint.class.getMethod("create", String.class, Object.class, WebServiceFeature[].class);
				return (Endpoint) ReflectionUtils.invokeMethod(create, null, bindingType, implementor, wsFeatures);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalStateException("JAX-WS 2.2 not available - cannot create feature endpoints", ex);
			}
		}

		private WebServiceFeature convertWebServiceFeature(Object feature) {
			Assert.notNull(feature, "WebServiceFeature specification object must not be null");
			if (feature instanceof WebServiceFeature) {
				return (WebServiceFeature) feature;
			}
			else if (feature instanceof Class) {
				return (WebServiceFeature) BeanUtils.instantiate((Class<?>) feature);
			}
			else if (feature instanceof String) {
				try {
					Class<?> featureClass = getBeanClassLoader().loadClass((String) feature);
					return (WebServiceFeature) BeanUtils.instantiate(featureClass);
				}
				catch (ClassNotFoundException ex) {
					throw new IllegalArgumentException("Could not load WebServiceFeature class [" + feature + "]");
				}
			}
			else {
				throw new IllegalArgumentException("Unknown WebServiceFeature specification type: " + feature.getClass());
			}
		}

		private ClassLoader getBeanClassLoader() {
			return (beanFactory instanceof ConfigurableBeanFactory ?
					((ConfigurableBeanFactory) beanFactory).getBeanClassLoader() : ClassUtils.getDefaultClassLoader());
		}
	}

}
