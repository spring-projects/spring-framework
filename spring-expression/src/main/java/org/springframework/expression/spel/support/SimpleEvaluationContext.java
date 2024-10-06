/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.expression.spel.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
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
 * of essential SpEL features and customization options, targeting simple
 * condition evaluation and in particular data binding scenarios.
 *
 * <p>In many cases, the full extent of the SpEL language is not required and
 * should be meaningfully restricted. Examples include but are not limited to
 * data binding expressions, property-based filters, and others. To that effect,
 * {@code SimpleEvaluationContext} is tailored to support only a subset of the
 * SpEL language syntax, e.g. excluding references to Java types, constructors,
 * and bean references.
 *
 * <p>When creating a {@code SimpleEvaluationContext} you need to choose the level of
 * support that you need for data binding in SpEL expressions:
 * <ul>
 * <li>Data binding for read-only access</li>
 * <li>Data binding for read and write access</li>
 * <li>A custom {@code PropertyAccessor} (typically not reflection-based), potentially
 * combined with a {@link DataBindingPropertyAccessor}</li>
 * </ul>
 *
 * <p>Conveniently, {@link SimpleEvaluationContext#forReadOnlyDataBinding()} enables
 * read-only access to properties via {@link DataBindingPropertyAccessor}. Similarly,
 * {@link SimpleEvaluationContext#forReadWriteDataBinding()} enables read and write access
 * to properties. Alternatively, configure custom accessors via
 * {@link SimpleEvaluationContext#forPropertyAccessors}, potentially
 * {@linkplain Builder#withAssignmentDisabled() disable assignment}, and optionally
 * activate method resolution and/or a type converter through the builder.
 *
 * <p>Note that {@code SimpleEvaluationContext} is typically not configured
 * with a default root object. Instead it is meant to be created once and
 * used repeatedly through {@code getValue} calls on a predefined
 * {@link org.springframework.expression.Expression} with both an
 * {@code EvaluationContext} and a root object as arguments:
 * {@link org.springframework.expression.Expression#getValue(EvaluationContext, Object)}.
 *
 * <p>For more power and flexibility, in particular for internal configuration
 * scenarios, consider using {@link StandardEvaluationContext} instead.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.3.15
 * @see #forReadOnlyDataBinding()
 * @see #forReadWriteDataBinding()
 * @see #forPropertyAccessors
 * @see StandardEvaluationContext
 * @see StandardTypeConverter
 * @see DataBindingPropertyAccessor
 */
public final class SimpleEvaluationContext implements EvaluationContext {

	private static final TypeLocator typeNotFoundTypeLocator = typeName -> {
		throw new SpelEvaluationException(SpelMessage.TYPE_NOT_FOUND, typeName);
	};


	private final TypedValue rootObject;

	private final List<PropertyAccessor> propertyAccessors;

	private final List<MethodResolver> methodResolvers;

	private final TypeConverter typeConverter;

	private final TypeComparator typeComparator = new StandardTypeComparator();

	private final OperatorOverloader operatorOverloader = new StandardOperatorOverloader();

	private final Map<String, Object> variables = new HashMap<>();

	private final boolean assignmentEnabled;


	private SimpleEvaluationContext(List<PropertyAccessor> accessors, List<MethodResolver> resolvers,
			@Nullable TypeConverter converter, @Nullable TypedValue rootObject, boolean assignmentEnabled) {

		this.propertyAccessors = accessors;
		this.methodResolvers = resolvers;
		this.typeConverter = (converter != null ? converter : new StandardTypeConverter());
		this.rootObject = (rootObject != null ? rootObject : TypedValue.NULL);
		this.assignmentEnabled = assignmentEnabled;
	}


	/**
	 * Return the specified root object, if any.
	 */
	@Override
	public TypedValue getRootObject() {
		return this.rootObject;
	}

	/**
	 * Return the specified {@link PropertyAccessor} delegates, if any.
	 * @see #forPropertyAccessors
	 */
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
	 * Return the specified {@link MethodResolver} delegates, if any.
	 * @see Builder#withMethodResolvers
	 */
	@Override
	public List<MethodResolver> getMethodResolvers() {
		return this.methodResolvers;
	}

	/**
	 * {@code SimpleEvaluationContext} does not support the use of bean references.
	 * @return always {@code null}
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
	 * @see Builder#withTypeConverter
	 * @see Builder#withConversionService
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

	/**
	 * {@code SimpleEvaluationContext} does not support variable assignment within
	 * expressions.
	 * @throws SpelEvaluationException with {@link SpelMessage#VARIABLE_ASSIGNMENT_NOT_SUPPORTED}
	 * @since 5.2.24
	 */
	@Override
	public TypedValue assignVariable(String name, Supplier<TypedValue> valueSupplier) {
		throw new SpelEvaluationException(SpelMessage.VARIABLE_ASSIGNMENT_NOT_SUPPORTED, "#" + name);
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
	 * Determine if assignment is enabled within expressions evaluated by this evaluation
	 * context.
	 * <p>If this method returns {@code false}, the assignment ({@code =}), increment
	 * ({@code ++}), and decrement ({@code --}) operators are disabled.
	 * @return {@code true} if assignment is enabled; {@code false} otherwise
	 * @since 5.3.38
	 * @see #forReadOnlyDataBinding()
	 * @see Builder#withAssignmentDisabled()
	 */
	@Override
	public boolean isAssignmentEnabled() {
		return this.assignmentEnabled;
	}

	/**
	 * Create a {@code SimpleEvaluationContext} for the specified {@link PropertyAccessor}
	 * delegates: typically a custom {@code PropertyAccessor} specific to a use case &mdash;
	 * for example, for attribute resolution in a custom data structure &mdash; potentially
	 * combined with a {@link DataBindingPropertyAccessor} if property dereferences are
	 * needed as well.
	 * <p>By default, assignment is enabled within expressions evaluated by the context
	 * created via this factory method; however, assignment can be disabled via
	 * {@link Builder#withAssignmentDisabled()}.
	 * @param accessors the accessor delegates to use
	 * @see DataBindingPropertyAccessor#forReadOnlyAccess()
	 * @see DataBindingPropertyAccessor#forReadWriteAccess()
	 * @see #isAssignmentEnabled()
	 * @see Builder#withAssignmentDisabled()
	 */
	public static Builder forPropertyAccessors(PropertyAccessor... accessors) {
		for (PropertyAccessor accessor : accessors) {
			if (accessor.getClass() == ReflectivePropertyAccessor.class) {
				throw new IllegalArgumentException("SimpleEvaluationContext is not designed for use with a plain " +
						"ReflectivePropertyAccessor. Consider using DataBindingPropertyAccessor or a custom subclass.");
			}
		}
		return new Builder(accessors);
	}

	/**
	 * Create a {@code SimpleEvaluationContext} for read-only access to
	 * public properties via {@link DataBindingPropertyAccessor}.
	 * <p>Assignment is disabled within expressions evaluated by the context created via
	 * this factory method.
	 * @see DataBindingPropertyAccessor#forReadOnlyAccess()
	 * @see #forPropertyAccessors
	 * @see #isAssignmentEnabled()
	 * @see Builder#withAssignmentDisabled()
	 */
	public static Builder forReadOnlyDataBinding() {
		return new Builder(DataBindingPropertyAccessor.forReadOnlyAccess()).withAssignmentDisabled();
	}

	/**
	 * Create a {@code SimpleEvaluationContext} for read-write access to
	 * public properties via {@link DataBindingPropertyAccessor}.
	 * <p>By default, assignment is enabled within expressions evaluated by the context
	 * created via this factory method. Assignment can be disabled via
	 * {@link Builder#withAssignmentDisabled()}; however, it is preferable to use
	 * {@link #forReadOnlyDataBinding()} if you desire read-only access.
	 * @see DataBindingPropertyAccessor#forReadWriteAccess()
	 * @see #forPropertyAccessors
	 * @see #isAssignmentEnabled()
	 * @see Builder#withAssignmentDisabled()
	 */
	public static Builder forReadWriteDataBinding() {
		return new Builder(DataBindingPropertyAccessor.forReadWriteAccess());
	}


	/**
	 * Builder for {@code SimpleEvaluationContext}.
	 */
	public static final class Builder {

		private final List<PropertyAccessor> accessors;

		private List<MethodResolver> resolvers = Collections.emptyList();

		@Nullable
		private TypeConverter typeConverter;

		@Nullable
		private TypedValue rootObject;

		private boolean assignmentEnabled = true;


		private Builder(PropertyAccessor... accessors) {
			this.accessors = Arrays.asList(accessors);
		}


		/**
		 * Disable assignment within expressions evaluated by this evaluation context.
		 * @since 5.3.38
		 * @see SimpleEvaluationContext#isAssignmentEnabled()
		 */
		public Builder withAssignmentDisabled() {
			this.assignmentEnabled = false;
			return this;
		}

		/**
		 * Register the specified {@link MethodResolver} delegates for
		 * a combination of property access and method resolution.
		 * @param resolvers the resolver delegates to use
		 * @see #withInstanceMethods()
		 * @see SimpleEvaluationContext#forPropertyAccessors
		 */
		public Builder withMethodResolvers(MethodResolver... resolvers) {
			for (MethodResolver resolver : resolvers) {
				if (resolver.getClass() == ReflectiveMethodResolver.class) {
					throw new IllegalArgumentException("SimpleEvaluationContext is not designed for use with a plain " +
							"ReflectiveMethodResolver. Consider using DataBindingMethodResolver or a custom subclass.");
				}
			}
			this.resolvers = Arrays.asList(resolvers);
			return this;
		}

		/**
		 * Register a {@link DataBindingMethodResolver} for instance method invocation purposes
		 * (i.e. not supporting static methods) in addition to the specified property accessors,
		 * typically in combination with a {@link DataBindingPropertyAccessor}.
		 * @see #withMethodResolvers
		 * @see SimpleEvaluationContext#forReadOnlyDataBinding()
		 * @see SimpleEvaluationContext#forReadWriteDataBinding()
		 */
		public Builder withInstanceMethods() {
			this.resolvers = Collections.singletonList(DataBindingMethodResolver.forInstanceMethodInvocation());
			return this;
		}

		/**
		 * Register a custom {@link ConversionService}.
		 * <p>By default a {@link StandardTypeConverter} backed by a
		 * {@link org.springframework.core.convert.support.DefaultConversionService} is used.
		 * @see #withTypeConverter
		 * @see StandardTypeConverter#StandardTypeConverter(ConversionService)
		 */
		public Builder withConversionService(ConversionService conversionService) {
			this.typeConverter = new StandardTypeConverter(conversionService);
			return this;
		}

		/**
		 * Register a custom {@link TypeConverter}.
		 * <p>By default a {@link StandardTypeConverter} backed by a
		 * {@link org.springframework.core.convert.support.DefaultConversionService} is used.
		 * @see #withConversionService
		 * @see StandardTypeConverter#StandardTypeConverter()
		 */
		public Builder withTypeConverter(TypeConverter converter) {
			this.typeConverter = converter;
			return this;
		}

		/**
		 * Specify a default root object to resolve against.
		 * <p>Default is none, expecting an object argument at evaluation time.
		 * @see org.springframework.expression.Expression#getValue(EvaluationContext)
		 * @see org.springframework.expression.Expression#getValue(EvaluationContext, Object)
		 */
		public Builder withRootObject(Object rootObject) {
			this.rootObject = new TypedValue(rootObject);
			return this;
		}

		/**
		 * Specify a typed root object to resolve against.
		 * <p>Default is none, expecting an object argument at evaluation time.
		 * @see org.springframework.expression.Expression#getValue(EvaluationContext)
		 * @see org.springframework.expression.Expression#getValue(EvaluationContext, Object)
		 */
		public Builder withTypedRootObject(Object rootObject, TypeDescriptor typeDescriptor) {
			this.rootObject = new TypedValue(rootObject, typeDescriptor);
			return this;
		}

		public SimpleEvaluationContext build() {
			return new SimpleEvaluationContext(this.accessors, this.resolvers, this.typeConverter, this.rootObject,
					this.assignmentEnabled);
		}
	}

}
