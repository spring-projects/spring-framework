/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.expression.spel.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.TypedValue;
import org.springframework.util.Assert;

/**
 * Provides a default EvaluationContext implementation.
 *
 * <p>To resolved properties/methods/fields this context uses a reflection mechanism.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class StandardEvaluationContext implements EvaluationContext {

	private TypedValue rootObject;

	private final Map<String, Object> variables = new HashMap<String, Object>();

	private final List<ConstructorResolver> constructorResolvers = new ArrayList<ConstructorResolver>();

	private final List<MethodResolver> methodResolvers = new ArrayList<MethodResolver>();

	private final List<PropertyAccessor> propertyAccessors = new ArrayList<PropertyAccessor>();

	private TypeLocator typeLocator = new StandardTypeLocator();

	private TypeComparator typeComparator = new StandardTypeComparator();

	private TypeConverter typeConverter = new StandardTypeConverter();

	private OperatorOverloader operatorOverloader = new StandardOperatorOverloader();

	public StandardEvaluationContext() {
		this.methodResolvers.add(new ReflectiveMethodResolver());
		this.constructorResolvers.add(new ReflectiveConstructorResolver());
		this.propertyAccessors.add(new ReflectivePropertyResolver());
	}
	
	public StandardEvaluationContext(Object rootObject) {
		this();
		setRootObject(rootObject);
	}

	public void setRootObject(Object rootObject) {
		this.rootObject = new TypedValue(rootObject,TypeDescriptor.forObject(rootObject));
	}

	public void setRootObject(Object rootObject, TypeDescriptor typeDescriptor) {
		this.rootObject = new TypedValue(rootObject,typeDescriptor);
	}

	public TypedValue getRootObject() {
		return this.rootObject;
	}

	public void setVariable(String name, Object value) {
		this.variables.put(name, value);
	}
	
	public void setVariables(Map<String,Object> variables) {
		this.variables.putAll(variables);
	}

	public void registerFunction(String name, Method method) {
		this.variables.put(name, method);
	}

	public Object lookupVariable(String name) {
		return this.variables.get(name);
	}

	public void addConstructorResolver(ConstructorResolver resolver) {
		this.constructorResolvers.add(this.constructorResolvers.size() - 1, resolver);
	}

	public List<ConstructorResolver> getConstructorResolvers() {
		return this.constructorResolvers;
	}

	public void addMethodResolver(MethodResolver resolver) {
		this.methodResolvers.add(this.methodResolvers.size() - 1, resolver);
	}

	public List<MethodResolver> getMethodResolvers() {
		return this.methodResolvers;
	}

	public void addPropertyAccessor(PropertyAccessor accessor) {
		this.propertyAccessors.add(this.propertyAccessors.size() - 1, accessor);
	}

	public List<PropertyAccessor> getPropertyAccessors() {
		return this.propertyAccessors;
	}

	public void setTypeLocator(TypeLocator typeLocator) {
		Assert.notNull(typeLocator, "TypeLocator must not be null");
		this.typeLocator = typeLocator;
	}

	public TypeLocator getTypeLocator() {
		return this.typeLocator;
	}

	public void setTypeComparator(TypeComparator typeComparator) {
		Assert.notNull(typeComparator, "TypeComparator must not be null");
		this.typeComparator = typeComparator;
	}

	public TypeComparator getTypeComparator() {
		return this.typeComparator;
	}

	public void setTypeConverter(TypeConverter typeConverter) {
		Assert.notNull(typeConverter, "TypeConverter must not be null");
		this.typeConverter = typeConverter;
	}

	public TypeConverter getTypeConverter() {
		return this.typeConverter;
	}

	public void setOperatorOverloader(OperatorOverloader operatorOverloader) {
		Assert.notNull(operatorOverloader, "OperatorOverloader must not be null");
		this.operatorOverloader = operatorOverloader;
	}

	public OperatorOverloader getOperatorOverloader() {
		return this.operatorOverloader;
	}

}
