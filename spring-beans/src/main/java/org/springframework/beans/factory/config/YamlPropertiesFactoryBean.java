/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.FactoryBean;

/**
 * Factory for Java Properties that reads from a YAML source. YAML is a nice
 * human-readable format for configuration, and it has some useful hierarchical
 * properties. It's more or less a superset of JSON, so it has a lot of similar
 * features. The Properties created by this factory have nested paths for
 * hierarchical objects, so for instance this YAML
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
 * is transformed into these Properties:
 *
 * <pre class="code">
 * environments.dev.url=http://dev.bar.com
 * environments.dev.name=Developer Setup
 * environments.prod.url=http://foo.bar.com
 * environments.prod.name=My Cool App
 * </pre>
 *
 * Lists are represented as comma-separated values (useful for simple String
 * values) and also as property keys with <code>[]</code> dereferencers, for
 * example this YAML:
 *
 * <pre class="code">
 * servers:
 * - dev.bar.com
 * - foo.bar.com
 * </pre>
 *
 * becomes java Properties like this:
 *
 * <pre class="code">
 * servers=dev.bar.com,foo.bar.com
 * servers[0]=dev.bar.com
 * servers[1]=foo.bar.com
 * </pre>
 *
 * Can create a singleton or a new object on each request. Default is
 * a singleton.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @since 4.1
 */
public class YamlPropertiesFactoryBean extends YamlProcessor implements
		FactoryBean<Properties> {

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
	public final Properties getObject() {
		if (!this.singleton || this.singletonInstance == null) {
			this.singletonInstance = createProperties();
		}
		return this.singletonInstance;
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
		final Properties result = new Properties();
		process(new MatchCallback() {
			@Override
			public void process(Properties properties, Map<String, Object> map) {
				result.putAll(properties);
			}
		});
		return result;
	}

}
