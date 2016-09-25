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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Factory for a {@code Map} that reads from a YAML source, preserving the
 * YAML-declared value types and their structure.
 *
 * <p>YAML is a nice human-readable format for configuration, and it has some
 * useful hierarchical properties. It's more or less a superset of JSON, so it
 * has a lot of similar features.
 *
 * <p>If multiple resources are provided the later ones will override entries in
 * the earlier ones hierarchically; that is, all entries with the same nested key
 * of type {@code Map} at any depth are merged. For example:
 *
 * <pre class="code">
 * foo:
 *   bar:
 *    one: two
 * three: four
 * </pre>
 *
 * plus (later in the list)
 *
 * <pre class="code">
 * foo:
 *   bar:
 *    one: 2
 * five: six
 * </pre>
 *
 * results in an effective input of
 *
 * <pre class="code">
 * foo:
 *   bar:
 *    one: 2
 * three: four
 * five: six
 * </pre>
 *
 * Note that the value of "foo" in the first document is not simply replaced
 * with the value in the second, but its nested values are merged.
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @since 4.1
 */
public class YamlMapFactoryBean extends YamlProcessor implements FactoryBean<Map<String, Object>>, InitializingBean {

	private boolean singleton = true;

	private Map<String, Object> map;


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
			this.map = createMap();
		}
	}

	@Override
	public Map<String, Object> getObject() {
		return (this.map != null ? this.map : createMap());
	}

	@Override
	public Class<?> getObjectType() {
		return Map.class;
	}


	/**
	 * Template method that subclasses may override to construct the object
	 * returned by this factory.
	 * <p>Invoked lazily the first time {@link #getObject()} is invoked in
	 * case of a shared singleton; else, on each {@link #getObject()} call.
	 * <p>The default implementation returns the merged {@code Map} instance.
	 * @return the object returned by this factory
	 * @see #process(java.util.Map, MatchCallback)
	 */
	protected Map<String, Object> createMap() {
		final Map<String, Object> result = new LinkedHashMap<String, Object>();
		process(new MatchCallback() {
			@Override
			public void process(Properties properties, Map<String, Object> map) {
				merge(result, map);
			}
		});
		return result;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void merge(Map<String, Object> output, Map<String, Object> map) {
		for (Entry<String, Object> entry : map.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			Object existing = output.get(key);
			if (value instanceof Map && existing instanceof Map) {
				Map<String, Object> result = new LinkedHashMap<String, Object>((Map) existing);
				merge(result, (Map) value);
				output.put(key, result);
			}
			else {
				output.put(key, value);
			}
		}
	}

}
