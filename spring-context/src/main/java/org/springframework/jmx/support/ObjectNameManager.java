/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jmx.support;

import java.util.Hashtable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * Helper class for the creation of {@link javax.management.ObjectName} instances.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see javax.management.ObjectName#getInstance(String)
 */
public final class ObjectNameManager {

	private ObjectNameManager() {
	}


	/**
	 * Retrieve the {@link ObjectName} instance corresponding to the supplied name.
	 * @param name the name in {@code ObjectName} or {@code String} format
	 * @return the {@code ObjectName} instance
	 * @throws MalformedObjectNameException in case of an invalid object name specification
	 * @see ObjectName#ObjectName(String)
	 * @see ObjectName#getInstance(String)
	 */
	public static ObjectName getInstance(Object name) throws MalformedObjectNameException {
		if (name instanceof ObjectName objectName) {
			return objectName;
		}
		if (!(name instanceof String text)) {
			throw new MalformedObjectNameException("Invalid ObjectName value type [" +
					name.getClass().getName() + "]: only ObjectName and String supported.");
		}
		return getInstance(text);
	}

	/**
	 * Retrieve the {@code ObjectName} instance corresponding to the supplied name.
	 * @param objectName the {@code ObjectName} in {@code String} format
	 * @return the {@code ObjectName} instance
	 * @throws MalformedObjectNameException in case of an invalid object name specification
	 * @see ObjectName#ObjectName(String)
	 * @see ObjectName#getInstance(String)
	 */
	public static ObjectName getInstance(String objectName) throws MalformedObjectNameException {
		return ObjectName.getInstance(objectName);
	}

	/**
	 * Retrieve an {@code ObjectName} instance for the specified domain and a
	 * single property with the supplied key and value.
	 * @param domainName the domain name for the {@code ObjectName}
	 * @param key the key for the single property in the {@code ObjectName}
	 * @param value the value for the single property in the {@code ObjectName}
	 * @return the {@code ObjectName} instance
	 * @throws MalformedObjectNameException in case of an invalid object name specification
	 * @see ObjectName#ObjectName(String, String, String)
	 * @see ObjectName#getInstance(String, String, String)
	 */
	public static ObjectName getInstance(String domainName, String key, String value)
			throws MalformedObjectNameException {

		return ObjectName.getInstance(domainName, key, value);
	}

	/**
	 * Retrieve an {@code ObjectName} instance with the specified domain name
	 * and the supplied key/name properties.
	 * @param domainName the domain name for the {@code ObjectName}
	 * @param properties the properties for the {@code ObjectName}
	 * @return the {@code ObjectName} instance
	 * @throws MalformedObjectNameException in case of an invalid object name specification
	 * @see ObjectName#ObjectName(String, java.util.Hashtable)
	 * @see ObjectName#getInstance(String, java.util.Hashtable)
	 */
	public static ObjectName getInstance(String domainName, Hashtable<String, String> properties)
			throws MalformedObjectNameException {

		return ObjectName.getInstance(domainName, properties);
	}

}
