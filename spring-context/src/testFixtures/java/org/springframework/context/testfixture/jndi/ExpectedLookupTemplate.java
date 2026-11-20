/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.jndi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;

import org.springframework.jndi.JndiTemplate;

/**
 * Copy of the standard {@link org.springframework.context.testfixture.jndi.jndi.ExpectedLookupTemplate}
 * for testing purposes.
 *
 * <p>Simple extension of the JndiTemplate class that always returns a given object.
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
	 * but honor only requests for the given name.
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
