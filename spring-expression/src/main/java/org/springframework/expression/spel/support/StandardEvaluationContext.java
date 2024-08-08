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

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.IndexAccessor;
import org.springframework.expression.MethodFilter;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.TypedValue;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A powerful and highly configurable {@link EvaluationContext} implementation.
 *
 * <p>This context uses standard implementations of all applicable strategies,
 * based on reflection to resolve properties, methods, and fields. Note, however,
 * that you may need to manually configure a {@code StandardTypeLocator} with a
 * specific {@link ClassLoader} to ensure that the SpEL expression parser is able
 * to reliably locate user types. See {@link #setTypeLocator(TypeLocator)} for
 * details.
 *
 * <p>In addition to support for setting and looking up variables as defined in
 * the {@link EvaluationContext} API, {@code StandardEvaluationContext} also
 * provides support for registering and looking up functions. The
 * {@code registerFunction(...)} methods provide a convenient way to register a
 * function as a {@link Method} or a {@link MethodHandle}; however, a function
 * can also be registered via {@link #setVariable(String, Object)} or
 * {@link #setVariables(Map)}. Since functions share a namespace with the variables
 * in this evaluation context, care must be taken to ensure that function names
 * and variable names do not overlap.
 *
 * <p>For a simpler, builder-style context variant for data-binding purposes,
 * consider using {@link SimpleEvaluationContext} instead which allows for
 * opting into several SpEL features as needed by specific use cases.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @since 3.0
 * @see SimpleEvaluationContext
 * @see ReflectivePropertyAccessor
 * @see ReflectiveConstructorResolver
 * @see ReflectiveMethodResolver
 * @see StandardTypeLocator
 * @see StandardTypeConverter
 * @see StandardTypeComparator
 * @see StandardOperatorOverloader
 */
public class StandardEvaluationContext implements EvaluationContext {

	private TypedValue rootObject;

	@Nullable
	private volatile List<PropertyAccessor> propertyAccessors;

	@Nullable
	private volatile List<IndexAccessor> indexAccessors;

	@Nullable
	private volatile List<ConstructorResolver> constructorResolvers;

	@Nullable
	private volatile List<MethodResolver> methodResolvers;

	@Nullable
	private volatile ReflectiveMethodResolver reflectiveMethodResolver;

	@Nullable
	private BeanResolver beanResolver;

	@Nullable
	private TypeLocator typeLocator;

	@Nullable
	private TypeConverter typeConverter;

	private TypeComparator typeComparator = StandardTypeComparator.INSTANCE;

	private OperatorOverloader operatorOverloader = StandardOperatorOverloader.INSTANCE;

	private final Map<String, Object> variables = new ConcurrentHashMap<>();


	/**
	 * Create a {@code StandardEvaluationContext} with a null root object.
	 */
	public StandardEvaluationContext() {
		this.rootObject = TypedValue.NULL;
	}

	/**
	 * Create a {@code StandardEvaluationContext} with the given root object.
	 * @param rootObject the root object to use
	 * @see #setRootObject
	 */
	public StandardEvaluationContext(@Nullable Object rootObject) {
		this.rootObject = new TypedValue(rootObject);
	}


	public void setRootObject(@Nullable Object rootObject, TypeDescriptor typeDescriptor) {
		this.rootObject = new TypedValue(rootObject, typeDescriptor);
	}

	public void setRootObject(@Nullable Object rootObject) {
		this.rootObject = (rootObject != null ? new TypedValue(rootObject) : TypedValue.NULL);
	}

	@Override
	public TypedValue getRootObject() {
		return this.rootObject;
	}

	public void setPropertyAccessors(List<PropertyAccessor> propertyAccessors) {
		this.propertyAccessors = propertyAccessors;
	}

	@Override
	public List<PropertyAccessor> getPropertyAccessors() {
		return initPropertyAccessors();
	}

	public void addPropertyAccessor(PropertyAccessor accessor) {
		addBeforeDefault(initPropertyAccessors(), accessor);
	}

	public boolean removePropertyAccessor(PropertyAccessor accessor) {
		return initPropertyAccessors().remove(accessor);
	}

	/**
	 * Set the list of index accessors to use in this evaluation context.
	 * <p>Replaces any previously configured index accessors.
	 * @since 6.2
	 * @see #getIndexAccessors()
	 * @see #addIndexAccessor(IndexAccessor)
	 * @see #removeIndexAccessor(IndexAccessor)
	 */
	public void setIndexAccessors(List<IndexAccessor> indexAccessors) {
		this.indexAccessors = indexAccessors;
	}

	/**
	 * Get the list of index accessors configured in this evaluation context.
	 * @since 6.2
	 * @see #setIndexAccessors(List)
	 * @see #addIndexAccessor(IndexAccessor)
	 * @see #removeIndexAccessor(IndexAccessor)
	 */
	@Override
	public List<IndexAccessor> getIndexAccessors() {
		return initIndexAccessors();
	}

	/**
	 * Add the supplied index accessor to this evaluation context.
	 * @param indexAccessor the index accessor to add
	 * @since 6.2
	 * @see #getIndexAccessors()
	 * @see #setIndexAccessors(List)
	 * @see #removeIndexAccessor(IndexAccessor)
	 */
	public void addIndexAccessor(IndexAccessor indexAccessor) {
		initIndexAccessors().add(indexAccessor);
	}

	/**
	 * Remove the supplied index accessor from this evaluation context.
	 * @param indexAccessor the index accessor to remove
	 * @return {@code true} if the index accessor was removed, {@code false} if
	 * the index accessor was not configured in this evaluation context
	 * @since 6.2
	 * @see #getIndexAccessors()
	 * @see #setIndexAccessors(List)
	 * @see #addIndexAccessor(IndexAccessor)
	 */
	public boolean removeIndexAccessor(IndexAccessor indexAccessor) {
		return initIndexAccessors().remove(indexAccessor);
	}

	public void setConstructorResolvers(List<ConstructorResolver> constructorResolvers) {
		this.constructorResolvers = constructorResolvers;
	}

	@Override
	public List<ConstructorResolver> getConstructorResolvers() {
		return initConstructorResolvers();
	}

	public void addConstructorResolver(ConstructorResolver resolver) {
		addBeforeDefault(initConstructorResolvers(), resolver);
	}

	public boolean removeConstructorResolver(ConstructorResolver resolver) {
		return initConstructorResolvers().remove(resolver);
	}

	public void setMethodResolvers(List<MethodResolver> methodResolvers) {
		this.methodResolvers = methodResolvers;
	}

	@Override
	public List<MethodResolver> getMethodResolvers() {
		return initMethodResolvers();
	}

	public void addMethodResolver(MethodResolver resolver) {
		addBeforeDefault(initMethodResolvers(), resolver);
	}

	public boolean removeMethodResolver(MethodResolver methodResolver) {
		return initMethodResolvers().remove(methodResolver);
	}

	public void setBeanResolver(BeanResolver beanResolver) {
		this.beanResolver = beanResolver;
	}

	@Override
	@Nullable
	public BeanResolver getBeanResolver() {
		return this.beanResolver;
	}

	/**
	 * Set the {@link TypeLocator} to use to find types, either by short or
	 * fully-qualified name.
	 * <p>By default, a {@link StandardTypeLocator} will be used.
	 * <p><strong>NOTE</strong>: Even if a {@code StandardTypeLocator} is
	 * sufficient, you may need to manually configure a {@code StandardTypeLocator}
	 * with a specific {@link ClassLoader} to ensure that the SpEL expression
	 * parser is able to reliably locate user types.
	 * @param typeLocator the {@code TypeLocator} to use
	 * @see StandardTypeLocator#StandardTypeLocator(ClassLoader)
	 * @see #getTypeLocator()
	 */
	public void setTypeLocator(TypeLocator typeLocator) {
		Assert.notNull(typeLocator, "TypeLocator must not be null");
		this.typeLocator = typeLocator;
	}

	/**
	 * Get the configured {@link TypeLocator} that will be used to find types,
	 * either by short or fully-qualified name.
	 * <p>See {@link #setTypeLocator(TypeLocator)} for further details.
	 * @see #setTypeLocator(TypeLocator)
	 */
	@Override
	public TypeLocator getTypeLocator() {
		if (this.typeLocator == null) {
			this.typeLocator = new StandardTypeLocator();
		}
		return this.typeLocator;
	}

	public void setTypeConverter(TypeConverter typeConverter) {
		Assert.notNull(typeConverter, "TypeConverter must not be null");
		this.typeConverter = typeConverter;
	}

	@Override
	public TypeConverter getTypeConverter() {
		if (this.typeConverter == null) {
			this.typeConverter = new StandardTypeConverter();
		}
		return this.typeConverter;
	}

	public void setTypeComparator(TypeComparator typeComparator) {
		Assert.notNull(typeComparator, "TypeComparator must not be null");
		this.typeComparator = typeComparator;
	}

	@Override
	public TypeComparator getTypeComparator() {
		return this.typeComparator;
	}

	public void setOperatorOverloader(OperatorOverloader operatorOverloader) {
		Assert.notNull(operatorOverloader, "OperatorOverloader must not be null");
		this.operatorOverloader = operatorOverloader;
	}

	@Override
	public OperatorOverloader getOperatorOverloader() {
		return this.operatorOverloader;
	}

	/**
	 * Set a named variable in this evaluation context to a specified value.
	 * <p>If the specified {@code name} is {@code null}, it will be ignored. If
	 * the specified {@code value} is {@code null}, the named variable will be
	 * removed from this evaluation context.
	 * <p>In contrast to {@link #assignVariable(String,java.util.function.Supplier)},
	 * this method should only be invoked programmatically when interacting directly
	 * with the {@code EvaluationContext} &mdash; for example, to provide initial
	 * configuration for the context.
	 * <p>Note that variables and functions share a common namespace in this
	 * evaluation context. See the {@linkplain StandardEvaluationContext
	 * class-level documentation} for details.
	 * @param name the name of the variable to set
	 * @param value the value to be placed in the variable
	 * @see #setVariables(Map)
	 * @see #registerFunction(String, Method)
	 * @see #registerFunction(String, MethodHandle)
	 * @see #lookupVariable(String)
	 */
	@Override
	public void setVariable(@Nullable String name, @Nullable Object value) {
		// For backwards compatibility, we ignore null names here...
		// And since ConcurrentHashMap cannot store null values, we simply take null
		// as a remove from the Map (with the same result from lookupVariable below).
		if (name != null) {
			if (value != null) {
				this.variables.put(name, value);
			}
			else {
				this.variables.remove(name);
			}
		}
	}

	/**
	 * Set multiple named variables in this evaluation context to the specified values.
	 * <p>This is a convenience variant of {@link #setVariable(String, Object)}.
	 * <p>Note that variables and functions share a common namespace in this
	 * evaluation context. See the {@linkplain StandardEvaluationContext
	 * class-level documentation} for details.
	 * @param variables the names and values of the variables to set
	 * @see #setVariable(String, Object)
	 */
	public void setVariables(Map<String, Object> variables) {
		variables.forEach(this::setVariable);
	}

	/**
	 * Register the specified {@link Method} as a SpEL function.
	 * <p>Note that variables and functions share a common namespace in this
	 * evaluation context. See the {@linkplain StandardEvaluationContext
	 * class-level documentation} for details.
	 * @param name the name of the function
	 * @param method the {@code Method} to register
	 * @see #registerFunction(String, MethodHandle)
	 */
	public void registerFunction(String name, Method method) {
		this.variables.put(name, method);
	}

	/**
	 * Register the specified {@link MethodHandle} as a SpEL function.
	 * <p>Note that variables and functions share a common namespace in this
	 * evaluation context. See the {@linkplain StandardEvaluationContext
	 * class-level documentation} for details.
	 * @param name the name of the function
	 * @param methodHandle the {@link MethodHandle} to register
	 * @since 6.1
	 * @see #registerFunction(String, Method)
	 */
	public void registerFunction(String name, MethodHandle methodHandle) {
		this.variables.put(name, methodHandle);
	}

	/**
	 * Look up a named variable or function within this evaluation context.
	 * <p>Note that variables and functions share a common namespace in this
	 * evaluation context. See the {@linkplain StandardEvaluationContext
	 * class-level documentation} for details.
	 * @param name the name of the variable or function to look up
	 * @return the value of the variable or function, or {@code null} if not found
	 */
	@Override
	@Nullable
	public Object lookupVariable(String name) {
		return this.variables.get(name);
	}

	/**
	 * Register a {@code MethodFilter} which will be called during method resolution
	 * for the specified type.
	 * <p>The {@code MethodFilter} may remove methods and/or sort the methods which
	 * will then be used by SpEL as the candidates to look through for a match.
	 * @param type the type for which the filter should be called
	 * @param filter a {@code MethodFilter}, or {@code null} to unregister a filter for the type
	 * @throws IllegalStateException if the {@link ReflectiveMethodResolver} is not in use
	 */
	public void registerMethodFilter(Class<?> type, MethodFilter filter) throws IllegalStateException {
		initMethodResolvers();
		ReflectiveMethodResolver resolver = this.reflectiveMethodResolver;
		if (resolver == null) {
			throw new IllegalStateException(
					"Method filter cannot be set as the reflective method resolver is not in use");
		}
		resolver.registerMethodFilter(type, filter);
	}

	/**
	 * Apply the internal delegates of this instance to the specified
	 * {@code evaluationContext}. Typically invoked right after the new context
	 * instance has been created to reuse the delegates. Does not modify the
	 * {@linkplain #setRootObject(Object) root object} or any registered
	 * {@linkplain #setVariable variables or functions}.
	 * @param evaluationContext the evaluation context to update
	 * @since 6.1.1
	 */
	public void applyDelegatesTo(StandardEvaluationContext evaluationContext) {
		// Triggers initialization for default delegates
		evaluationContext.setConstructorResolvers(new ArrayList<>(getConstructorResolvers()));
		evaluationContext.setMethodResolvers(new ArrayList<>(getMethodResolvers()));
		evaluationContext.setPropertyAccessors(new ArrayList<>(getPropertyAccessors()));
		evaluationContext.setIndexAccessors(new ArrayList<>(getIndexAccessors()));
		evaluationContext.setTypeLocator(getTypeLocator());
		evaluationContext.setTypeConverter(getTypeConverter());

		evaluationContext.beanResolver = this.beanResolver;
		evaluationContext.operatorOverloader = this.operatorOverloader;
		evaluationContext.reflectiveMethodResolver = this.reflectiveMethodResolver;
		evaluationContext.typeComparator = this.typeComparator;
	}


	private List<PropertyAccessor> initPropertyAccessors() {
		List<PropertyAccessor> accessors = this.propertyAccessors;
		if (accessors == null) {
			accessors = new ArrayList<>(5);
			accessors.add(new ReflectivePropertyAccessor());
			this.propertyAccessors = accessors;
		}
		return accessors;
	}

	private List<IndexAccessor> initIndexAccessors() {
		List<IndexAccessor> accessors = this.indexAccessors;
		if (accessors == null) {
			accessors = new ArrayList<>(5);
			this.indexAccessors = accessors;
		}
		return accessors;
	}

	private List<ConstructorResolver> initConstructorResolvers() {
		List<ConstructorResolver> resolvers = this.constructorResolvers;
		if (resolvers == null) {
			resolvers = new ArrayList<>(1);
			resolvers.add(new ReflectiveConstructorResolver());
			this.constructorResolvers = resolvers;
		}
		return resolvers;
	}

	private List<MethodResolver> initMethodResolvers() {
		List<MethodResolver> resolvers = this.methodResolvers;
		if (resolvers == null) {
			resolvers = new ArrayList<>(1);
			this.reflectiveMethodResolver = new ReflectiveMethodResolver();
			resolvers.add(this.reflectiveMethodResolver);
			this.methodResolvers = resolvers;
		}
		return resolvers;
	}

	private static <T> void addBeforeDefault(List<T> list, T element) {
		list.add(list.size() - 1, element);
	}

}
