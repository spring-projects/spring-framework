/*
 * Copyright 2004-2008 the original author or authors.
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
package org.springframework.expression.spel.standard;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.TypeUtils;
import org.springframework.expression.spel.reflection.ReflectionConstructorResolver;
import org.springframework.expression.spel.reflection.ReflectionMethodResolver;
import org.springframework.expression.spel.reflection.ReflectionPropertyResolver;

/**
 * Provides a default EvaluationContext implementation.
 * <p>
 * To resolved properties/methods/fields this context uses a reflection mechanism.
 * 
 * @author Andy Clement
 * 
 */
public class StandardEvaluationContext implements EvaluationContext {

	private Object rootObject;
	private StandardTypeUtilities typeUtils;
	private final Map<String, Object> variables = new HashMap<String, Object>();
	private final List<MethodResolver> methodResolvers = new ArrayList<MethodResolver>();
	private final List<ConstructorResolver> constructorResolvers = new ArrayList<ConstructorResolver>();
	private final List<PropertyAccessor> propertyResolvers = new ArrayList<PropertyAccessor>();
	private final Map<String, Map<String, Object>> simpleReferencesMap = new HashMap<String, Map<String, Object>>();

	public StandardEvaluationContext() {
		typeUtils = new StandardTypeUtilities();
		addMethodResolver(new ReflectionMethodResolver());
		addConstructorResolver(new ReflectionConstructorResolver());
		addPropertyAccessor(new ReflectionPropertyResolver());
	}

	public void reset() {
		typeUtils = new StandardTypeUtilities();
		methodResolvers.clear();
		addMethodResolver(new ReflectionMethodResolver());
		constructorResolvers.clear();
		addConstructorResolver(new ReflectionConstructorResolver());
		propertyResolvers.clear();
		addPropertyAccessor(new ReflectionPropertyResolver());
		simpleReferencesMap.clear();
		variables.clear();
		rootObject = null;
	}

	public StandardEvaluationContext(Object rootContextObject) {
		this();
		rootObject = rootContextObject;
	}

	public void setClassLoader(ClassLoader loader) {
		TypeLocator tLocator = typeUtils.getTypeLocator();
		if (tLocator instanceof StandardTypeLocator) {
			((StandardTypeLocator) tLocator).setClassLoader(loader);
		}
	}

	public void registerImport(String importPrefix) {
		TypeLocator tLocator = typeUtils.getTypeLocator();
		if (tLocator instanceof StandardTypeLocator) {
			((StandardTypeLocator) tLocator).registerImport(importPrefix);
		}
	}

	public void setClasspath(String classpath) {
		StringTokenizer st = new StringTokenizer(classpath, File.pathSeparator);
		List<URL> urls = new ArrayList<URL>();
		while (st.hasMoreTokens()) {
			String element = st.nextToken();
			try {
				urls.add(new File(element).toURI().toURL());
			} catch (MalformedURLException e) {
				throw new RuntimeException("Invalid element in classpath " + element);
			}
		}
		ClassLoader cl = new URLClassLoader(urls.toArray(new URL[] {}), Thread.currentThread().getContextClassLoader());
		TypeLocator tLocator = typeUtils.getTypeLocator();
		if (tLocator instanceof StandardTypeLocator) {
			((StandardTypeLocator) tLocator).setClassLoader(cl);
		}
	}

	public Object lookupVariable(String name) {
		return variables.get(name);
	}

	public TypeUtils getTypeUtils() {
		return typeUtils;
	}

	public Object getRootContextObject() {
		return rootObject;
	}

	public Object lookupReference(Object contextName, Object objectName) {
		String contextToLookup = (contextName == null ? "root" : (String) contextName);
		// if (contextName==null) return simpleReferencesMap;
		Map<String, Object> contextMap = simpleReferencesMap.get(contextToLookup);
		if (contextMap == null)
			return null;
		if (objectName == null)
			return contextMap;
		return contextMap.get(objectName);
	}

	public List<PropertyAccessor> getPropertyAccessors() {
		return propertyResolvers;
	}

	public void addPropertyAccessor(PropertyAccessor accessor) {
		propertyResolvers.add(accessor);
	}

	public void removePropertyAccessor(PropertyAccessor accessor) {
		propertyResolvers.remove(accessor);
	}

	public void insertPropertyAccessor(int position, PropertyAccessor accessor) {
		propertyResolvers.add(position, accessor);
	}

	public List<MethodResolver> getMethodResolvers() {
		return methodResolvers;
	}

	public List<ConstructorResolver> getConstructorResolvers() {
		return constructorResolvers;
	}

	public void setVariable(String name, Object value) {
		variables.put(name, value);
	}

	public void registerFunction(String name, Method m) {
		variables.put(name, m);
	}

	public void setRootObject(Object o) {
		rootObject = o;
	}

	// TODO have a variant that adds at position (same for ctor/propOrField)
	public void addMethodResolver(MethodResolver resolver) {
		methodResolvers.add(resolver);
	}

	public void removeMethodResolver(MethodResolver resolver) {
		methodResolvers.remove(resolver);
	}

	public void insertMethodResolver(int pos, MethodResolver resolver) {
		methodResolvers.add(pos, resolver);
	}

	public void addConstructorResolver(ConstructorResolver resolver) {
		constructorResolvers.add(resolver);
	}

	public void addReference(String contextName, String objectName, Object value) {
		Map<String, Object> contextMap = simpleReferencesMap.get(contextName);
		if (contextMap == null) {
			contextMap = new HashMap<String, Object>();
			simpleReferencesMap.put(contextName, contextMap);
		}
		contextMap.put(objectName, value);
	}

	public void addTypeConverter(StandardIndividualTypeConverter newConverter) {
		((StandardTypeConverter) typeUtils.getTypeConverter()).registerConverter(newConverter);
	}

}
