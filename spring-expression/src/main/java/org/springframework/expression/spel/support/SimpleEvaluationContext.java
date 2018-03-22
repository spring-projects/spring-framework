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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.ConversionService;
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
 * <p>In many cases, the full extent of the SpEL language is not required and
 * should be meaningfully restricted. Examples include but are not limited to
 * data binding expressions, property-based filters, and others. To that effect,
 * {@code SimpleEvaluationContext} is tailored to support only a subset of the
 * SpEL language syntax, e.g. excluding references to Java types, constructors,
 * and bean references.
 *
 * <p>When creating {@code SimpleEvaluationContext} you need to choose the level
 * of support you need to deal with properties and methods in SpEL expressions:
 * <ul>
 * <li>Custom {@code PropertyAccessor} only (no reflection)</li>
 * <li>Data binding properties for read-only access</li>
 * <li>Data binding properties for read and write</li>
 * </ul>
 *
 * <p>Conveniently, {@link SimpleEvaluationContext#forReadOnlyDataBinding()}
 * enables read access to properties via {@link DataBindingPropertyAccessor};
 * same for {@link SimpleEvaluationContext#forReadWriteDataBinding()} when
 * write access is needed as well. Alternatively, configure custom accessors
 * via {@link SimpleEvaluationContext#forPropertyAccessors}.
 *
 * <p>Note that {@code SimpleEvaluationContext} cannot be configured with
 * a default root object. Instead it is meant to be created once and used
 * repeatedly through {@code getValue} calls on a pre-compiled
 * {@link org.springframework.expression.Expression} with both an
 * {@code EvaluationContext} and a root object as arguments
 *
 * <p>For more flexibility, consider {@link StandardEvaluationContext} instead.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.3.15
 * @see #forReadOnlyDataBinding()
 * @see #forReadWriteDataBinding()
 * @see StandardEvaluationContext
 * @see StandardTypeConverter
 * @see DataBindingPropertyAccessor
 */
public class SimpleEvaluationContext implements EvaluationContext {

	private static final TypeLocator typeNotFoundTypeLocator = typeName -> {
		throw new SpelEvaluationException(SpelMessage.TYPE_NOT_FOUND, typeName);
	};


	private final List<PropertyAccessor> propertyAccessors;

	private final TypeConverter typeConverter;

	private final TypeComparator typeComparator = new StandardTypeComparator();

	private final OperatorOverloader operatorOverloader = new StandardOperatorOverloader();

	private final Map<String, Object> variables = new HashMap<>();


	private SimpleEvaluationContext(List<PropertyAccessor> accessors, @Nullable TypeConverter converter) {
		this.propertyAccessors = accessors;
		this.typeConverter = (converter != null ? converter : new StandardTypeConverter());
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
		return Collections.emptyList();
	}

	/**
	 * Return a single {@link ReflectiveMethodResolver}.
	 */
	@Override
	public List<MethodResolver> getMethodResolvers() {
		return Collections.emptyList();
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
	 * Create a {@code SimpleEvaluationContext} for the specified
	 * {@link PropertyAccessor} delegates.
	 * @see ReflectivePropertyAccessor
	 * @see DataBindingPropertyAccessor
	 */
	public static Builder forPropertyAccessors(PropertyAccessor... accessors) {
		return new Builder(accessors);
	}

	/**
	 * Create a {@code SimpleEvaluationContext} for read-only access to
	 * public properties via {@link DataBindingPropertyAccessor}.
	 * @see DataBindingPropertyAccessor#forReadOnlyAccess()
	 */
	public static Builder forReadOnlyDataBinding() {
		return new Builder(DataBindingPropertyAccessor.forReadOnlyAccess());
	}

	/**
	 * Create a {@code SimpleEvaluationContext} for read-write access to
	 * public properties via {@link DataBindingPropertyAccessor}.
	 * @see DataBindingPropertyAccessor#forReadOnlyAccess()
	 */
	public static Builder forReadWriteDataBinding() {
		return new Builder(DataBindingPropertyAccessor.forReadWriteAccess());
	}


	/**
	 * Builder for {@code SimpleEvaluationContext}.
	 */
	public static class Builder {

		private final List<PropertyAccessor> propertyAccessors;

		@Nullable
		private TypeConverter typeConverter;

		public Builder(PropertyAccessor... accessors) {
			this.propertyAccessors = Arrays.asList(accessors);
		}

		/**
		 * Register a custom {@link TypeConverter}.
		 * <p>By default a {@link StandardTypeConverter} backed by a
		 * {@link org.springframework.core.convert.support.DefaultConversionService}
		 * is used.
		 * @see #withConversionService
		 * @see StandardTypeConverter#StandardTypeConverter()
		 */
		public Builder withTypeConverter(TypeConverter converter) {
			this.typeConverter = converter;
			return this;
		}

		/**
		 * Register a custom {@link ConversionService}.
		 * <p>By default a {@link StandardTypeConverter} backed by a
		 * {@link org.springframework.core.convert.support.DefaultConversionService}
		 * is used.
		 * @see #withTypeConverter
		 * @see StandardTypeConverter#StandardTypeConverter(ConversionService)
		 */
		public Builder withConversionService(ConversionService conversionService) {
			this.typeConverter = new StandardTypeConverter(conversionService);
			return this;
		}

		public SimpleEvaluationContext build() {
			return new SimpleEvaluationContext(this.propertyAccessors, this.typeConverter);
		}
	}

}
