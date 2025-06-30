/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.validation;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;

/**
 * Default implementation of the {@link Errors} and {@link BindingResult}
 * interfaces, for the registration and evaluation of binding errors on
 * JavaBean objects.
 *
 * <p>Performs standard JavaBean property access, also supporting nested
 * properties. Normally, application code will work with the
 * {@code Errors} interface or the {@code BindingResult} interface.
 * A {@link DataBinder} returns its {@code BindingResult} via
 * {@link DataBinder#getBindingResult()}.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see DataBinder#getBindingResult()
 * @see DataBinder#initBeanPropertyAccess()
 * @see DirectFieldBindingResult
 */
@SuppressWarnings("serial")
public class BeanPropertyBindingResult extends AbstractPropertyBindingResult implements Serializable {

	private final @Nullable Object target;

	private final boolean autoGrowNestedPaths;

	private final int autoGrowCollectionLimit;

	private transient @Nullable BeanWrapper beanWrapper;


	/**
	 * Create a new {@code BeanPropertyBindingResult} for the given target.
	 * @param target the target bean to bind onto
	 * @param objectName the name of the target object
	 */
	public BeanPropertyBindingResult(@Nullable Object target, String objectName) {
		this(target, objectName, true, Integer.MAX_VALUE);
	}

	/**
	 * Create a new {@code BeanPropertyBindingResult} for the given target.
	 * @param target the target bean to bind onto
	 * @param objectName the name of the target object
	 * @param autoGrowNestedPaths whether to "auto-grow" a nested path that contains a null value
	 * @param autoGrowCollectionLimit the limit for array and collection auto-growing
	 */
	public BeanPropertyBindingResult(@Nullable Object target, String objectName,
			boolean autoGrowNestedPaths, int autoGrowCollectionLimit) {

		super(objectName);
		this.target = target;
		this.autoGrowNestedPaths = autoGrowNestedPaths;
		this.autoGrowCollectionLimit = autoGrowCollectionLimit;
	}


	@Override
	public final @Nullable Object getTarget() {
		return this.target;
	}

	/**
	 * Returns the {@link BeanWrapper} that this instance uses.
	 * Creates a new one if none existed before.
	 * @see #createBeanWrapper()
	 */
	@Override
	public final ConfigurablePropertyAccessor getPropertyAccessor() {
		if (this.beanWrapper == null) {
			this.beanWrapper = createBeanWrapper();
			this.beanWrapper.setExtractOldValueForEditor(true);
			this.beanWrapper.setAutoGrowNestedPaths(this.autoGrowNestedPaths);
			this.beanWrapper.setAutoGrowCollectionLimit(this.autoGrowCollectionLimit);
		}
		return this.beanWrapper;
	}

	/**
	 * Create a new {@link BeanWrapper} for the underlying target object.
	 * @see #getTarget()
	 */
	protected BeanWrapper createBeanWrapper() {
		if (this.target == null) {
			throw new IllegalStateException("Cannot access properties on null bean instance '" + getObjectName() + "'");
		}
		return PropertyAccessorFactory.forBeanPropertyAccess(this.target);
	}

}
