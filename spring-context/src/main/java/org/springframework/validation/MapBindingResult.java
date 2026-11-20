/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.validation;

import java.io.Serializable;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Map-based implementation of the BindingResult interface,
 * supporting registration and evaluation of binding errors on
 * Map attributes.
 *
 * <p>Can be used as errors holder for custom binding onto a
 * Map, for example when invoking a Validator for a Map object.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see java.util.Map
 */
@SuppressWarnings("serial")
public class MapBindingResult extends AbstractBindingResult implements Serializable {

	private final Map<?, ?> target;


	/**
	 * Create a new MapBindingResult instance.
	 * @param target the target Map to bind onto
	 * @param objectName the name of the target object
	 */
	public MapBindingResult(Map<?, ?> target, String objectName) {
		super(objectName);
		Assert.notNull(target, "Target Map must not be null");
		this.target = target;
	}


	/**
	 * Return the target Map to bind onto.
	 */
	public final Map<?, ?> getTargetMap() {
		return this.target;
	}

	@Override
	public final Object getTarget() {
		return this.target;
	}

	@Override
	protected @Nullable Object getActualFieldValue(String field) {
		return this.target.get(field);
	}

}
