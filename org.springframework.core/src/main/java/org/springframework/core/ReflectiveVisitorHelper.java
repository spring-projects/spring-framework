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

package org.springframework.core;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.util.CachingMapDecorator;
import org.springframework.util.ReflectionUtils;

/**
 * Helper implementation for a reflective visitor.
 * Mainly for internal use within the framework.
 *
 * <p>To use, call <code>invokeVisit</code>, passing a Visitor object
 * and the data argument to accept (double-dispatch). For example:
 * 
 * <pre>
 *   public String styleValue(Object value) {
 *     reflectiveVistorSupport.invokeVisit(this, value)
 *   }
 *  
 *   // visit call back will be invoked via reflection
 *   String visit(&lt;valueType&gt; arg) {
 *     // process argument of type &lt;valueType&gt;
 *   }
 * </pre>
 *
 * See the {@link org.springframework.core.style.DefaultValueStyler} class
 * for a concrete usage of this visitor helper.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.2.2
 * @deprecated as of Spring 2.5, to be removed in Spring 3.0
 */
public class ReflectiveVisitorHelper {

	private static final String VISIT_METHOD = "visit";

	private static final String VISIT_NULL = "visitNull";

	private static final Log logger = LogFactory.getLog(ReflectiveVisitorHelper.class);


	private final CachingMapDecorator visitorClassVisitMethods = new CachingMapDecorator() {
		public Object create(Object key) {
			return new ClassVisitMethods((Class) key);
		}
	};


	/**
	 * Use reflection to call the appropriate <code>visit</code> method
	 * on the provided visitor, passing in the specified argument.
	 * @param visitor the visitor encapsulating the logic to process the argument
	 * @param argument the argument to dispatch
	 * @throws IllegalArgumentException if the visitor parameter is null
	 */
	public Object invokeVisit(Object visitor, Object argument) {
		Assert.notNull(visitor, "The visitor to visit is required");
		// Perform call back on the visitor through reflection.
		Method method = getMethod(visitor.getClass(), argument);
		if (method == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("No method found by reflection for visitor class [" + visitor.getClass().getName()
						+ "] and argument of type [" + (argument != null ? argument.getClass().getName() : "") + "]");
			}
			return null;
		}
		try {
			Object[] args = null;
			if (argument != null) {
				args = new Object[] {argument};
			}
			if (!Modifier.isPublic(method.getModifiers())) {
				method.setAccessible(true);
			}
			return method.invoke(visitor, args);
		}
		catch (Exception ex) {
			ReflectionUtils.handleReflectionException(ex);
			throw new IllegalStateException("Should never get here");
		}
	}

	/**
	 * Determines the most appropriate visit method for the
	 * given visitor class and argument.
	 */
	private Method getMethod(Class visitorClass, Object argument) {
		ClassVisitMethods visitMethods = (ClassVisitMethods) this.visitorClassVisitMethods.get(visitorClass);
		return visitMethods.getVisitMethod(argument != null ? argument.getClass() : null);
	}


	/**
	 * Internal class caching visitor methods by argument class.
	 */
	private static class ClassVisitMethods {

		private final Class visitorClass;

		private final CachingMapDecorator visitMethodCache = new CachingMapDecorator() {
			public Object create(Object argumentClazz) {
				if (argumentClazz == null) {
					return findNullVisitorMethod();
				}
				Method method = findVisitMethod((Class) argumentClazz);
				if (method == null) {
					method = findDefaultVisitMethod();
				}
				return method;
			}
		};

		public ClassVisitMethods(Class visitorClass) {
			this.visitorClass = visitorClass;
		}

		private Method findNullVisitorMethod() {
			for (Class clazz = this.visitorClass; clazz != null; clazz = clazz.getSuperclass()) {
				try {
					return clazz.getDeclaredMethod(VISIT_NULL, (Class[]) null);
				}
				catch (NoSuchMethodException ex) {
				}
			}
			return findDefaultVisitMethod();
		}

		private Method findDefaultVisitMethod() {
			final Class[] args = {Object.class};
			for (Class clazz = this.visitorClass; clazz != null; clazz = clazz.getSuperclass()) {
				try {
					return clazz.getDeclaredMethod(VISIT_METHOD, args);
				}
				catch (NoSuchMethodException ex) {
				}
			}
			if (logger.isWarnEnabled()) {
				logger.warn("No default '" + VISIT_METHOD + "' method found. Returning <null>.");
			}
			return null;
		}

		/**
		 * Gets a cached visitor method for the specified argument type.
		 */
		private Method getVisitMethod(Class argumentClass) {
			return (Method) this.visitMethodCache.get(argumentClass);
		}

		/**
		 * Traverses class hierarchy looking for applicable visit() method.
		 */
		private Method findVisitMethod(Class rootArgumentType) {
			if (rootArgumentType == Object.class) {
				return null;
			}
			LinkedList classQueue = new LinkedList();
			classQueue.addFirst(rootArgumentType);

			while (!classQueue.isEmpty()) {
				Class argumentType = (Class) classQueue.removeLast();
				// Check for a visit method on the visitor class matching this
				// argument type.
				try {
					if (logger.isTraceEnabled()) {
						logger.trace("Looking for method " + VISIT_METHOD + "(" + argumentType + ")");
					}
					return findVisitMethod(this.visitorClass, argumentType);
				}
				catch (NoSuchMethodException e) {
					// Queue up the argument super class if it's not of type Object.
					if (!argumentType.isInterface() && (argumentType.getSuperclass() != Object.class)) {
						classQueue.addFirst(argumentType.getSuperclass());
					}
					// Queue up argument's implemented interfaces.
					Class[] interfaces = argumentType.getInterfaces();
					for (int i = 0; i < interfaces.length; i++) {
						classQueue.addFirst(interfaces[i]);
					}
				}
			}
			// No specific method found -> return the default.
			return findDefaultVisitMethod();
		}

		private Method findVisitMethod(Class visitorClass, Class argumentType) throws NoSuchMethodException {
			try {
				return visitorClass.getDeclaredMethod(VISIT_METHOD, new Class[] {argumentType});
			}
			catch (NoSuchMethodException ex) {
				// Try visitorClass superclasses.
				if (visitorClass.getSuperclass() != Object.class) {
					return findVisitMethod(visitorClass.getSuperclass(), argumentType);
				}
				else {
					throw ex;
				}
			}
		}
	}

}
