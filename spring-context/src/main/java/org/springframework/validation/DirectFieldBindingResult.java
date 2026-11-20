/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.validation;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.AbstractNestablePropertyAccessor;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;

/**
 * Special implementation of the Errors and BindingResult interfaces,
 * supporting registration and evaluation of binding errors on value objects.
 * Performs direct field access instead of going through JavaBean getters.
 *
 * <p>Since Spring 4.1 this implementation is able to traverse nested fields.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see DataBinder#getBindingResult()
 * @see DataBinder#initDirectFieldAccess()
 * @see BeanPropertyBindingResult
 */
@SuppressWarnings("serial")
public class DirectFieldBindingResult extends AbstractPropertyBindingResult {

	private final @Nullable Object target;

	private final boolean autoGrowNestedPaths;

	private final int autoGrowCollectionLimit;

	private transient @Nullable ConfigurablePropertyAccessor directFieldAccessor;


	/**
	 * Create a new {@code DirectFieldBindingResult} for the given target.
	 * @param target the target object to bind onto
	 * @param objectName the name of the target object
	 */
	public DirectFieldBindingResult(@Nullable Object target, String objectName) {
		this(target, objectName, true);
	}

	/**
	 * Create a new {@code DirectFieldBindingResult} for the given target.
	 * @param target the target object to bind onto
	 * @param objectName the name of the target object
	 * @param autoGrowNestedPaths whether to "auto-grow" a nested path that contains a null value
	 */
	public DirectFieldBindingResult(@Nullable Object target, String objectName, boolean autoGrowNestedPaths) {
		this(target, objectName, autoGrowNestedPaths, Integer.MAX_VALUE);
	}

	/**
	 * Create a new {@code DirectFieldBindingResult} for the given target.
	 * @param target the target object to bind onto
	 * @param objectName the name of the target object
	 * @param autoGrowNestedPaths whether to "auto-grow" a nested path that contains a null value
	 * @param autoGrowCollectionLimit the limit for array and collection auto-growing
	 * @since 7.0.11
	 */
	public DirectFieldBindingResult(@Nullable Object target, String objectName,
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
	 * Returns the DirectFieldAccessor that this instance uses.
	 * Creates a new one if none existed before.
	 * @see #createDirectFieldAccessor()
	 */
	@Override
	public final ConfigurablePropertyAccessor getPropertyAccessor() {
		if (this.directFieldAccessor == null) {
			this.directFieldAccessor = createDirectFieldAccessor();
			this.directFieldAccessor.setExtractOldValueForEditor(true);
			this.directFieldAccessor.setAutoGrowNestedPaths(this.autoGrowNestedPaths);
			if (this.directFieldAccessor instanceof AbstractNestablePropertyAccessor accessor) {
				accessor.setAutoGrowCollectionLimit(this.autoGrowCollectionLimit);
			}
		}
		return this.directFieldAccessor;
	}

	/**
	 * Create a new DirectFieldAccessor for the underlying target object.
	 * @see #getTarget()
	 */
	protected ConfigurablePropertyAccessor createDirectFieldAccessor() {
		if (this.target == null) {
			throw new IllegalStateException("Cannot access fields on null target instance '" + getObjectName() + "'");
		}
		return PropertyAccessorFactory.forDirectFieldAccess(this.target);
	}

}
