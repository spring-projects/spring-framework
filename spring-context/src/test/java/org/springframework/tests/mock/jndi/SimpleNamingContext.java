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

package org.springframework.tests.mock.jndi;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.StringUtils;

/**
 * Simple implementation of a JNDI naming context.
 * Only supports binding plain Objects to String names.
 * Mainly for test environments, but also usable for standalone applications.
 *
 * <p>This class is not intended for direct usage by applications, although it
 * can be used for example to override JndiTemplate's {@code createInitialContext}
 * method in unit tests. Typically, SimpleNamingContextBuilder will be used to
 * set up a JVM-level JNDI environment.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see SimpleNamingContextBuilder
 * @see org.springframework.jndi.JndiTemplate#createInitialContext
 */
public class SimpleNamingContext implements Context {

	private final Log logger = LogFactory.getLog(getClass());

	private final String root;

	private final Hashtable<String, Object> boundObjects;

	private final Hashtable<String, Object> environment = new Hashtable<>();


	/**
	 * Create a new naming context.
	 */
	public SimpleNamingContext() {
		this("");
	}

	/**
	 * Create a new naming context with the given naming root.
	 */
	public SimpleNamingContext(String root) {
		this.root = root;
		this.boundObjects = new Hashtable<>();
	}

	/**
	 * Create a new naming context with the given naming root,
	 * the given name/object map, and the JNDI environment entries.
	 */
	public SimpleNamingContext(String root, Hashtable<String, Object> boundObjects, Hashtable<String, Object> env) {
		this.root = root;
		this.boundObjects = boundObjects;
		if (env != null) {
			this.environment.putAll(env);
		}
	}


	// Actual implementations of Context methods follow

	@Override
	public NamingEnumeration<NameClassPair> list(String root) throws NamingException {
		if (logger.isDebugEnabled()) {
			logger.debug("Listing name/class pairs under [" + root + "]");
		}
		return new NameClassPairEnumeration(this, root);
	}

	@Override
	public NamingEnumeration<Binding> listBindings(String root) throws NamingException {
		if (logger.isDebugEnabled()) {
			logger.debug("Listing bindings under [" + root + "]");
		}
		return new BindingEnumeration(this, root);
	}

	/**
	 * Look up the object with the given name.
	 * <p>Note: Not intended for direct use by applications.
	 * Will be used by any standard InitialContext JNDI lookups.
	 * @throws javax.naming.NameNotFoundException if the object could not be found
	 */
	@Override
	public Object lookup(String lookupName) throws NameNotFoundException {
		String name = this.root + lookupName;
		if (logger.isDebugEnabled()) {
			logger.debug("Static JNDI lookup: [" + name + "]");
		}
		if ("".equals(name)) {
			return new SimpleNamingContext(this.root, this.boundObjects, this.environment);
		}
		Object found = this.boundObjects.get(name);
		if (found == null) {
			if (!name.endsWith("/")) {
				name = name + "/";
			}
			for (String boundName : this.boundObjects.keySet()) {
				if (boundName.startsWith(name)) {
					return new SimpleNamingContext(name, this.boundObjects, this.environment);
				}
			}
			throw new NameNotFoundException(
					"Name [" + this.root + lookupName + "] not bound; " + this.boundObjects.size() + " bindings: [" +
					StringUtils.collectionToDelimitedString(this.boundObjects.keySet(), ",") + "]");
		}
		return found;
	}

	@Override
	public Object lookupLink(String name) throws NameNotFoundException {
		return lookup(name);
	}

	/**
	 * Bind the given object to the given name.
	 * Note: Not intended for direct use by applications
	 * if setting up a JVM-level JNDI environment.
	 * Use SimpleNamingContextBuilder to set up JNDI bindings then.
	 * @see org.springframework.tests.mock.jndi.SimpleNamingContextBuilder#bind
	 */
	@Override
	public void bind(String name, Object obj) {
		if (logger.isInfoEnabled()) {
			logger.info("Static JNDI binding: [" + this.root + name + "] = [" + obj + "]");
		}
		this.boundObjects.put(this.root + name, obj);
	}

	@Override
	public void unbind(String name) {
		if (logger.isInfoEnabled()) {
			logger.info("Static JNDI remove: [" + this.root + name + "]");
		}
		this.boundObjects.remove(this.root + name);
	}

	@Override
	public void rebind(String name, Object obj) {
		bind(name, obj);
	}

	@Override
	public void rename(String oldName, String newName) throws NameNotFoundException {
		Object obj = lookup(oldName);
		unbind(oldName);
		bind(newName, obj);
	}

	@Override
	public Context createSubcontext(String name) {
		String subcontextName = this.root + name;
		if (!subcontextName.endsWith("/")) {
			subcontextName += "/";
		}
		Context subcontext = new SimpleNamingContext(subcontextName, this.boundObjects, this.environment);
		bind(name, subcontext);
		return subcontext;
	}

	@Override
	public void destroySubcontext(String name) {
		unbind(name);
	}

	@Override
	public String composeName(String name, String prefix) {
		return prefix + name;
	}

	@Override
	public Hashtable<String, Object> getEnvironment() {
		return this.environment;
	}

	@Override
	public Object addToEnvironment(String propName, Object propVal) {
		return this.environment.put(propName, propVal);
	}

	@Override
	public Object removeFromEnvironment(String propName) {
		return this.environment.remove(propName);
	}

	@Override
	public void close() {
	}


	// Unsupported methods follow: no support for javax.naming.Name

	@Override
	public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
		throw new OperationNotSupportedException("SimpleNamingContext does not support [javax.naming.Name]");
	}

	@Override
	public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
		throw new OperationNotSupportedException("SimpleNamingContext does not support [javax.naming.Name]");
	}

	@Override
	public Object lookup(Name name) throws NamingException {
		throw new OperationNotSupportedException("SimpleNamingContext does not support [javax.naming.Name]");
	}

	@Override
	public Object lookupLink(Name name) throws NamingException {
		throw new OperationNotSupportedException("SimpleNamingContext does not support [javax.naming.Name]");
	}

	@Override
	public void bind(Name name, Object obj) throws NamingException {
		throw new OperationNotSupportedException("SimpleNamingContext does not support [javax.naming.Name]");
	}

	@Override
	public void unbind(Name name) throws NamingException {
		throw new OperationNotSupportedException("SimpleNamingContext does not support [javax.naming.Name]");
	}

	@Override
	public void rebind(Name name, Object obj) throws NamingException {
		throw new OperationNotSupportedException("SimpleNamingContext does not support [javax.naming.Name]");
	}

	@Override
	public void rename(Name oldName, Name newName) throws NamingException {
		throw new OperationNotSupportedException("SimpleNamingContext does not support [javax.naming.Name]");
	}

	@Override
	public Context createSubcontext(Name name) throws NamingException {
		throw new OperationNotSupportedException("SimpleNamingContext does not support [javax.naming.Name]");
	}

	@Override
	public void destroySubcontext(Name name) throws NamingException {
		throw new OperationNotSupportedException("SimpleNamingContext does not support [javax.naming.Name]");
	}

	@Override
	public String getNameInNamespace() throws NamingException {
		throw new OperationNotSupportedException("SimpleNamingContext does not support [javax.naming.Name]");
	}

	@Override
	public NameParser getNameParser(Name name) throws NamingException {
		throw new OperationNotSupportedException("SimpleNamingContext does not support [javax.naming.Name]");
	}

	@Override
	public NameParser getNameParser(String name) throws NamingException {
		throw new OperationNotSupportedException("SimpleNamingContext does not support [javax.naming.Name]");
	}

	@Override
	public Name composeName(Name name, Name prefix) throws NamingException {
		throw new OperationNotSupportedException("SimpleNamingContext does not support [javax.naming.Name]");
	}


	private static abstract class AbstractNamingEnumeration<T> implements NamingEnumeration<T> {

		private Iterator<T> iterator;

		private AbstractNamingEnumeration(SimpleNamingContext context, String proot) throws NamingException {
			if (!"".equals(proot) && !proot.endsWith("/")) {
				proot = proot + "/";
			}
			String root = context.root + proot;
			Map<String, T> contents = new HashMap<>();
			for (String boundName : context.boundObjects.keySet()) {
				if (boundName.startsWith(root)) {
					int startIndex = root.length();
					int endIndex = boundName.indexOf('/', startIndex);
					String strippedName =
							(endIndex != -1 ? boundName.substring(startIndex, endIndex) : boundName.substring(startIndex));
					if (!contents.containsKey(strippedName)) {
						try {
							contents.put(strippedName, createObject(strippedName, context.lookup(proot + strippedName)));
						}
						catch (NameNotFoundException ex) {
							// cannot happen
						}
					}
				}
			}
			if (contents.size() == 0) {
				throw new NamingException("Invalid root: [" + context.root + proot + "]");
			}
			this.iterator = contents.values().iterator();
		}

		protected abstract T createObject(String strippedName, Object obj);

		@Override
		public boolean hasMore() {
			return this.iterator.hasNext();
		}

		@Override
		public T next() {
			return this.iterator.next();
		}

		@Override
		public boolean hasMoreElements() {
			return this.iterator.hasNext();
		}

		@Override
		public T nextElement() {
			return this.iterator.next();
		}

		@Override
		public void close() {
		}
	}


	private static class NameClassPairEnumeration extends AbstractNamingEnumeration<NameClassPair> {

		private NameClassPairEnumeration(SimpleNamingContext context, String root) throws NamingException {
			super(context, root);
		}

		@Override
		protected NameClassPair createObject(String strippedName, Object obj) {
			return new NameClassPair(strippedName, obj.getClass().getName());
		}
	}


	private static class BindingEnumeration extends AbstractNamingEnumeration<Binding> {

		private BindingEnumeration(SimpleNamingContext context, String root) throws NamingException {
			super(context, root);
		}

		@Override
		protected Binding createObject(String strippedName, Object obj) {
			return new Binding(strippedName, obj);
		}
	}

}
