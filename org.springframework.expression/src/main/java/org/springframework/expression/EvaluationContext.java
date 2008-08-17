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
package org.springframework.expression;

import java.util.List;

import org.springframework.expression.spel.standard.StandardEvaluationContext;
import org.springframework.expression.spel.standard.StandardTypeUtilities;

/**
 * Expressions are executed in an evaluation context. It is in this context that references are resolved when
 * encountered during expression evaluation.
 * 
 * There is a default implementation of the EvaluationContext, {@link StandardEvaluationContext} that can be extended,
 * rather than having to implement everything.
 * 
 * @author Andy Clement
 */
public interface EvaluationContext {

	/**
	 * @return the root context object against which unqualified properties/methods/etc should be resolved
	 */
	Object getRootContextObject();

	/**
	 * @return a TypeUtilities implementation that can be used for looking up types, converting types, comparing types,
	 * and overloading basic operators for types. A standard implementation is provided in {@link StandardTypeUtilities}
	 */
	TypeUtils getTypeUtils();

	/**
	 * Look up a named variable within this execution context.
	 * 
	 * @param name variable to lookup
	 * @return the value of the variable
	 */
	Object lookupVariable(String name);

	/**
	 * Set a named variable within this execution context to a specified value.
	 * 
	 * @param name variable to set
	 * @param value value to be placed in the variable
	 */
	void setVariable(String name, Object value);

	// TODO lookupReference() - is it too expensive to return all objects within a context?
	/**
	 * Look up an object reference in a particular context. If no contextName is specified (null), assume the default
	 * context. If no objectName is specified (null), return all objects in the specified context (List<Object>).
	 * 
	 * @param contextName the context in which to perform the lookup (or null for default context)
	 * @param objectName the object to lookup in the context (or null to get all objects)
	 * @return a specific object or List<Object>
	 */
	Object lookupReference(Object contextName, Object objectName) throws EvaluationException;

	/**
	 * @return a list of resolvers that will be asked in turn to locate a constructor
	 */
	List<ConstructorResolver> getConstructorResolvers();

	/**
	 * @return a list of resolvers that will be asked in turn to locate a method
	 */
	List<MethodResolver> getMethodResolvers();

	/**
	 * @return a list of accessors that will be asked in turn to read/write a property
	 */
	List<PropertyAccessor> getPropertyAccessors();

}
