/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.Properties;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.CollectionFactory;

/**
 * Factory for {@link java.util.Properties} that reads from a YAML source,
 * exposing a flat structure of String property values.
 *
 * <p>YAML is a nice human-readable format for configuration, and it has some
 * useful hierarchical properties. It's more or less a superset of JSON, so it
 * has a lot of similar features.
 *
 * <p><b>Note: All exposed values are of type {@code String}</b> for access through
 * the common {@link Properties#getProperty} method (e.g. in configuration property
 * resolution through {@link PropertyResourceConfigurer#setProperties(Properties)}).
 * If this is not desirable, use {@link YamlMapFactoryBean} instead.
 *
 * <p>The Properties created by this factory have nested paths for hierarchical
 * objects, so for instance this YAML
 *
 * <pre class="code">
 * environments:
 *   dev:
 *     url: http://dev.bar.com
 *     name: Developer Setup
 *   prod:
 *     url: http://foo.bar.com
 *     name: My Cool App
 * </pre>
 *
 * is transformed into these properties:
 *
 * <pre class="code">
 * environments.dev.url=http://dev.bar.com
 * environments.dev.name=Developer Setup
 * environments.prod.url=http://foo.bar.com
 * environments.prod.name=My Cool App
 * </pre>
 *
 * Lists are split as property keys with <code>[]</code> dereferencers, for
 * example this YAML:
 *
 * <pre class="code">
 * servers:
 * - dev.bar.com
 * - foo.bar.com
 * </pre>
 *
 * becomes properties like this:
 *
 * <pre class="code">
 * servers[0]=dev.bar.com
 * servers[1]=foo.bar.com
 * </pre>
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 */
public class YamlPropertiesFactoryBean extends YamlProcessor implements FactoryBean<Properties>, InitializingBean {

	private boolean singleton = true;

	private Properties properties;


	/**
	 * Set if a singleton should be created, or a new object on each request
	 * otherwise. Default is {@code true} (a singleton).
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	@Override
	public boolean isSingleton() {
		return this.singleton;
	}

	@Override
	public void afterPropertiesSet() {
		if (isSingleton()) {
			this.properties = createProperties();
		}
	}

	@Override
	public Properties getObject() {
		return (this.properties != null ? this.properties : createProperties());
	}

	@Override
	public Class<?> getObjectType() {
		return Properties.class;
	}


	/**
	 * Template method that subclasses may override to construct the object
	 * returned by this factory. The default implementation returns a
	 * properties with the content of all resources.
	 * <p>Invoked lazily the first time {@link #getObject()} is invoked in
	 * case of a shared singleton; else, on each {@link #getObject()} call.
	 * @return the object returned by this factory
	 * @see #process(MatchCallback) ()
	 */
	protected Properties createProperties() {
		Properties result = CollectionFactory.createStringAdaptingProperties();
		process((properties, map) -> result.putAll(properties));
		return result;
	}

}
