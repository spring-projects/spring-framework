/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.config;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.TypeConverter;
import org.springframework.core.ResolvableType;

/**
 * Simple factory for shared List instances. Allows for central setup
 * of Lists via the "list" element in XML bean definitions.
 *
 * @author Juergen Hoeller
 * @since 09.12.2003
 * @see SetFactoryBean
 * @see MapFactoryBean
 */
public class ListFactoryBean extends AbstractFactoryBean<List<Object>> {

	private @Nullable List<?> sourceList;

	@SuppressWarnings("rawtypes")
	private @Nullable Class<? extends List> targetListClass;


	/**
	 * Set the source List, typically populated via XML "list" elements.
	 */
	public void setSourceList(List<?> sourceList) {
		this.sourceList = sourceList;
	}

	/**
	 * Set the class to use for the target List. Can be populated with a fully
	 * qualified class name when defined in a Spring application context.
	 * <p>Default is a {@code java.util.ArrayList}.
	 * @see java.util.ArrayList
	 */
	@SuppressWarnings("rawtypes")
	public void setTargetListClass(@Nullable Class<? extends List> targetListClass) {
		if (targetListClass == null) {
			throw new IllegalArgumentException("'targetListClass' must not be null");
		}
		if (!List.class.isAssignableFrom(targetListClass)) {
			throw new IllegalArgumentException("'targetListClass' must implement [java.util.List]");
		}
		this.targetListClass = targetListClass;
	}


	@Override
	@SuppressWarnings("rawtypes")
	public Class<List> getObjectType() {
		return List.class;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<Object> createInstance() {
		if (this.sourceList == null) {
			throw new IllegalArgumentException("'sourceList' is required");
		}
		List<Object> result = null;
		if (this.targetListClass != null) {
			result = BeanUtils.instantiateClass(this.targetListClass);
		}
		else {
			result = new ArrayList<>(this.sourceList.size());
		}
		Class<?> valueType = null;
		if (this.targetListClass != null) {
			valueType = ResolvableType.forClass(this.targetListClass).asCollection().resolveGeneric();
		}
		if (valueType != null) {
			TypeConverter converter = getBeanTypeConverter();
			for (Object elem : this.sourceList) {
				result.add(converter.convertIfNecessary(elem, valueType));
			}
		}
		else {
			result.addAll(this.sourceList);
		}
		return result;
	}

}
