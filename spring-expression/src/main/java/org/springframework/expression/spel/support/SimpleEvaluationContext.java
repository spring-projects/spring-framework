/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.expression.BeanResolver;
import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;

/**
 * A basic implementation of {@link EvaluationContext} that focuses on a subset
 * of essential SpEL features and configuration options, and relies on default
 * strategies otherwise.
 *
 * <p>In many cases, the full extent of the SpEL is not
 * required and should be meaningfully restricted. Examples include but are not
 * limited to data binding expressions, property-based filters, and others. To
 * that effect, {@code SimpleEvaluationContext} supports only a subset of the
 * SpEL language syntax that excludes references to Java types, constructors,
 * and bean references.
 *
 * <p>Note that {@code SimpleEvaluationContext} cannot be configured with a
 * default root object. Instead it is meant to be created once and used
 * repeatedly through method variants on
 * {@link org.springframework.expression.Expression Expression} that accept
 * both an {@code EvaluationContext} and a root object.
 *
 * @author Rossen Stoyanchev
 * @since 4.3.15
 */
public class SimpleEvaluationContext implements EvaluationContext {

	private static final TypeLocator typeNotFoundTypeLocator = new TypeLocator() {

		@Override
		public Class<?> findType(String typeName) throws EvaluationException {
			throw new SpelEvaluationException(SpelMessage.TYPE_NOT_FOUND, typeName);
		}
	};


	private final List<PropertyAccessor> propertyAccessors;

	private final List<ConstructorResolver> constructorResolvers =
			Collections.<ConstructorResolver>singletonList(new ReflectiveConstructorResolver());

	private final List<MethodResolver> methodResolvers =
			Collections.<MethodResolver>singletonList(new ReflectiveMethodResolver());

	private final TypeConverter typeConverter;

	private final TypeComparator typeComparator = new StandardTypeComparator();

	private final OperatorOverloader operatorOverloader = new StandardOperatorOverloader();

	private final Map<String, Object> variables = new HashMap<String, Object>();


	public SimpleEvaluationContext() {
		this(null, null);
	}

	public SimpleEvaluationContext(List<PropertyAccessor> accessors, TypeConverter converter) {
		this.propertyAccessors = initPropertyAccessors(accessors);
		this.typeConverter = converter != null ? converter : new StandardTypeConverter();
	}


	private static List<PropertyAccessor> initPropertyAccessors(List<PropertyAccessor> accessors) {
		if (accessors == null) {
			accessors = new ArrayList<PropertyAccessor>(5);
			accessors.add(new ReflectivePropertyAccessor());
		}
		return accessors;
	}


	/**
	 * {@code SimpleEvaluationContext} cannot be configured with a root object.
	 * It is meant for repeated use with
	 * {@link org.springframework.expression.Expression Expression} method
	 * variants that accept both an {@code EvaluationContext} and a root object.
	 * @return Always returns {@link TypedValue#NULL}.
	 */
	@Override
	public TypedValue getRootObject() {
		return TypedValue.NULL;
	}

	@Override
	public List<PropertyAccessor> getPropertyAccessors() {
		return this.propertyAccessors;
	}

	/**
	 * Return a single {@link ReflectiveConstructorResolver}.
	 */
	@Override
	public List<ConstructorResolver> getConstructorResolvers() {
		return this.constructorResolvers;
	}

	/**
	 * Return a single {@link ReflectiveMethodResolver}.
	 */
	@Override
	public List<MethodResolver> getMethodResolvers() {
		return this.methodResolvers;
	}

	/**
	 * {@code SimpleEvaluationContext} does not support use of bean references.
	 * @return Always returns {@code null}
	 */
	@Override
	public BeanResolver getBeanResolver() {
		return null;
	}

	/**
	 * {@code SimpleEvaluationContext} does not support use of type references.
	 * @return {@code TypeLocator} implementation that raises a
	 * {@link SpelEvaluationException} with {@link SpelMessage#TYPE_NOT_FOUND}.
	 */
	@Override
	public TypeLocator getTypeLocator() {
		return typeNotFoundTypeLocator;
	}

	/**
	 * The configured {@link TypeConverter}.
	 * <p>By default this is {@link StandardTypeConverter}.
	 */
	@Override
	public TypeConverter getTypeConverter() {
		return this.typeConverter;
	}

	/**
	 * Return an instance of {@link StandardTypeComparator}.
	 */
	@Override
	public TypeComparator getTypeComparator() {
		return this.typeComparator;
	}


	/**
	 * Return an instance of {@link StandardOperatorOverloader}.
	 */
	@Override
	public OperatorOverloader getOperatorOverloader() {
		return this.operatorOverloader;
	}

	@Override
	public void setVariable(String name, Object value) {
		this.variables.put(name, value);
	}

	@Override
	public Object lookupVariable(String name) {
		return this.variables.get(name);
	}

}
