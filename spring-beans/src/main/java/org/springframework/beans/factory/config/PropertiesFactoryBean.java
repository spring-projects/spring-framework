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

package org.springframework.beans.factory.config;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.support.PropertiesLoaderSupport;

/**
 * Allows for making a properties file from a classpath location available
 * as Properties instance in a bean factory. Can be used to populate
 * any bean property of type Properties via a bean reference.
 *
 * <p>Supports loading from a properties file and/or setting local properties
 * on this FactoryBean. The created Properties instance will be merged from
 * loaded and local values. If neither a location nor local properties are set,
 * an exception will be thrown on initialization.
 *
 * <p>Can create a singleton or a new object on each request.
 * Default is a singleton.
 *
 * @author Juergen Hoeller
 * @see #setLocation
 * @see #setProperties
 * @see #setLocalOverride
 * @see java.util.Properties
 */
public class PropertiesFactoryBean extends PropertiesLoaderSupport
		implements FactoryBean<Properties>, InitializingBean {

	private boolean singleton = true;

	private Properties singletonInstance;


	/**
	 * Set whether a shared 'singleton' Properties instance should be
	 * created, or rather a new Properties instance on each request.
	 * <p>Default is "true" (a shared singleton).
	 */
	public final void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	@Override
	public final boolean isSingleton() {
		return this.singleton;
	}


	@Override
	public final void afterPropertiesSet() throws IOException {
		if (this.singleton) {
			this.singletonInstance = createProperties();
		}
	}

	@Override
	public final Properties getObject() throws IOException {
		if (this.singleton) {
			return this.singletonInstance;
		}
		else {
			return createProperties();
		}
	}

	@Override
	public Class<Properties> getObjectType() {
		return Properties.class;
	}


	/**
	 * Template method that subclasses may override to construct the object
	 * returned by this factory. The default implementation returns the
	 * plain merged Properties instance.
	 * <p>Invoked on initialization of this FactoryBean in case of a
	 * shared singleton; else, on each {@link #getObject()} call.
	 * @return the object returned by this factory
	 * @throws IOException if an exception occured during properties loading
	 * @see #mergeProperties()
	 */
	protected Properties createProperties() throws IOException {
		return (Properties) createInstance();
	}

	/**
	 * Template method that subclasses may override to construct the object
	 * returned by this factory. The default implementation returns the
	 * plain merged Properties instance.
	 * <p>Invoked on initialization of this FactoryBean in case of a
	 * shared singleton; else, on each {@link #getObject()} call.
	 * @return the object returned by this factory
	 * @throws IOException if an exception occured during properties loading
	 * @deprecated as of Spring 3.0, in favor of {@link #createProperties()}
	 */
	@Deprecated
	protected Object createInstance() throws IOException {
		return mergeProperties();
	}

}
