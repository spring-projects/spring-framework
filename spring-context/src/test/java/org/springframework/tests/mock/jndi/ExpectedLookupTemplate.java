/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.tests.mock.jndi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;

import org.springframework.jndi.JndiTemplate;

/**
 * Simple extension of the JndiTemplate class that always returns a given object.
 *
 * <p>Very useful for testing. Effectively a mock object.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class ExpectedLookupTemplate extends JndiTemplate {

	private final Map<String, Object> jndiObjects = new ConcurrentHashMap<>(16);


	/**
	 * Construct a new JndiTemplate that will always return given objects for
	 * given names. To be populated through {@code addObject} calls.
	 * @see #addObject(String, Object)
	 */
	public ExpectedLookupTemplate() {
	}

	/**
	 * Construct a new JndiTemplate that will always return the given object,
	 * but honour only requests for the given name.
	 * @param name the name the client is expected to look up
	 * @param object the object that will be returned
	 */
	public ExpectedLookupTemplate(String name, Object object) {
		addObject(name, object);
	}


	/**
	 * Add the given object to the list of JNDI objects that this template will expose.
	 * @param name the name the client is expected to look up
	 * @param object the object that will be returned
	 */
	public void addObject(String name, Object object) {
		this.jndiObjects.put(name, object);
	}

	/**
	 * If the name is the expected name specified in the constructor, return the
	 * object provided in the constructor. If the name is unexpected, a
	 * respective NamingException gets thrown.
	 */
	@Override
	public Object lookup(String name) throws NamingException {
		Object object = this.jndiObjects.get(name);
		if (object == null) {
			throw new NamingException("Unexpected JNDI name '" + name + "': expecting " + this.jndiObjects.keySet());
		}
		return object;
	}

}
