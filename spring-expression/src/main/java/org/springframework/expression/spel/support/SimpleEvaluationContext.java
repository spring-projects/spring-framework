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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.expression.BeanResolver;
import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.lang.Nullable;

/**
 * A basic implementation of {@link EvaluationContext} that focuses on a subset
 * of essential SpEL features and configuration options.
 *
 * <p>In many cases, the full extent of the SpEL language is not
 * required and should be meaningfully restricted. Examples include but are not
 * limited to data binding expressions, property-based filters, and others. To
 * that effect, {@code SimpleEvaluationContext} is tailored to support only a
 * subset of the SpEL language syntax, e.g. excluding references to Java types,
 * constructors, and bean references.
 *
 * <p>When creating {@code SimpleEvaluationContext} you need to choose the level
 * of support you need to deal with properties and methods in SpEL expressions.
 * By default, {@link SimpleEvaluationContext#create()} enables only read access
 * to properties via {@link DataBindingPropertyAccessor}. Alternatively, use
 * {@link SimpleEvaluationContext#builder()} to configure the exact level of
 * support needed, targeting one of, or some combination of the following:
 * <ul>
 * <li>Custom {@code PropertyAccessor} only (no reflection).</li>
 * <li>Data binding properties for read-only access.</li>
 * <li>Data binding properties for read and write.</li>
 * </ul>
 *
 * <p>Note that {@code SimpleEvaluationContext} cannot be configured with a
 * default root object. Instead it is meant to be created once and used
 * repeatedly through method variants on
 * {@link org.springframework.expression.Expression Expression} that accept
 * both an {@code EvaluationContext} and a root object.
 *
 * @author Rossen Stoyanchev
 * @since 4.3.15
 * @see StandardEvaluationContext
 * @see DataBindingPropertyAccessor
 */
public class SimpleEvaluationContext implements EvaluationContext {

	private static final TypeLocator typeNotFoundTypeLocator = typeName -> {
		throw new SpelEvaluationException(SpelMessage.TYPE_NOT_FOUND, typeName);
	};


	private final List<PropertyAccessor> propertyAccessors;

	private final List<ConstructorResolver> constructorResolvers = Collections.emptyList();

	private final List<MethodResolver> methodResolvers = Collections.emptyList();

	private final TypeConverter typeConverter;

	private final TypeComparator typeComparator = new StandardTypeComparator();

	private final OperatorOverloader operatorOverloader = new StandardOperatorOverloader();

	private final Map<String, Object> variables = new HashMap<>();


	private SimpleEvaluationContext(List<PropertyAccessor> accessors, @Nullable TypeConverter converter) {
		this.propertyAccessors = Collections.unmodifiableList(new ArrayList<>(accessors));
		this.typeConverter = converter != null ? converter : new StandardTypeConverter();
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
	 * Return an empty list, always, since this context does not support the
	 * use of type references.
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
	@Nullable
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
	public void setVariable(String name, @Nullable Object value) {
		this.variables.put(name, value);
	}

	@Override
	@Nullable
	public Object lookupVariable(String name) {
		return this.variables.get(name);
	}


	/**
	 * Create a {@code SimpleEvaluationContext} with read-only access to
	 * public properties via {@link DataBindingPropertyAccessor}.
	 * <p>Effectively, a shortcut for:
	 * <pre class="code">
	 * SimpleEvaluationContext context = SimpleEvaluationContext.builder()
	 *         .dataBindingPropertyAccessor(true)
	 *         .build();
	 * </pre>
	 * @see #builder()
	 */
	public static SimpleEvaluationContext create() {
		return new Builder().dataBindingPropertyAccessor(true).build();
	}

	/**
	 * Return a builder to create a {@code SimpleEvaluationContext}.
	 * @see #create()
	 */
	public static Builder builder() {
		return new Builder();
	}


	/**
	 * Builder for {@code SimpleEvaluationContext}.
	 */
	public static class Builder {

		private final List<PropertyAccessor> propertyAccessors = new ArrayList<>();

		@Nullable
		private TypeConverter typeConverter;


		/**
		 * Enable access to public properties for data binding purposes.
		 * <p>Effectively, a shortcut for
		 * {@code propertyAccessor(new DataBindingPropertyAccessor(boolean))}.
		 * @param readOnlyAccess whether to read-only access to properties,
		 * {@code "true"}, or read and write, {@code "false"}.
		 */
		public Builder dataBindingPropertyAccessor(boolean readOnlyAccess) {
			return propertyAccessor(readOnlyAccess ?
					DataBindingPropertyAccessor.forReadOnlyAccess() :
					DataBindingPropertyAccessor.forReadWriteAccess());
		}

		/**
		 * Register a custom accessor for properties in expressions.
		 * <p>By default, the builder does not enable property access.
		 */
		public Builder propertyAccessor(PropertyAccessor... accessors) {
			this.propertyAccessors.addAll(Arrays.asList(accessors));
			return this;
		}

		/**
		 * Register a custom {@link TypeConverter}.
		 * <p>By default {@link StandardTypeConverter} is used.
		 */
		public Builder typeConverter(TypeConverter converter) {
			this.typeConverter = converter;
			return this;
		}

		public SimpleEvaluationContext build() {
			return new SimpleEvaluationContext(this.propertyAccessors, this.typeConverter);
		}
	}

}
