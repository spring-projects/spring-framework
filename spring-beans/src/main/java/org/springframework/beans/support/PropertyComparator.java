/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.beans.support;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * PropertyComparator performs a comparison of two beans,
 * evaluating the specified bean property via a BeanWrapper.
 *
 * @author Juergen Hoeller
 * @author Jean-Pierre Pawlak
 * @since 19.05.2003
 * @param <T> the type of objects that may be compared by this comparator
 * @see org.springframework.beans.BeanWrapper
 */
public class PropertyComparator<T> implements Comparator<T> {

	protected final Log logger = LogFactory.getLog(getClass());

	private final SortDefinition sortDefinition;


	/**
	 * Create a new PropertyComparator for the given SortDefinition.
	 * @see MutableSortDefinition
	 */
	public PropertyComparator(SortDefinition sortDefinition) {
		this.sortDefinition = sortDefinition;
	}

	/**
	 * Create a PropertyComparator for the given settings.
	 * @param property the property to compare
	 * @param ignoreCase whether upper and lower case in String values should be ignored
	 * @param ascending whether to sort ascending (true) or descending (false)
	 */
	public PropertyComparator(String property, boolean ignoreCase, boolean ascending) {
		this.sortDefinition = new MutableSortDefinition(property, ignoreCase, ascending);
	}

	/**
	 * Return the SortDefinition that this comparator uses.
	 */
	public final SortDefinition getSortDefinition() {
		return this.sortDefinition;
	}


	@Override
	@SuppressWarnings("unchecked")
	public int compare(T o1, T o2) {
		Object v1 = getPropertyValue(o1);
		Object v2 = getPropertyValue(o2);
		if (this.sortDefinition.isIgnoreCase() && (v1 instanceof String) && (v2 instanceof String)) {
			v1 = ((String) v1).toLowerCase();
			v2 = ((String) v2).toLowerCase();
		}

		int result;

		// Put an object with null property at the end of the sort result.
		try {
			if (v1 != null) {
				result = (v2 != null ? ((Comparable<Object>) v1).compareTo(v2) : -1);
			}
			else {
				result = (v2 != null ? 1 : 0);
			}
		}
		catch (RuntimeException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not sort objects [" + o1 + "] and [" + o2 + "]", ex);
			}
			return 0;
		}

		return (this.sortDefinition.isAscending() ? result : -result);
	}

	/**
	 * Get the SortDefinition's property value for the given object.
	 * @param obj the object to get the property value for
	 * @return the property value
	 */
	@Nullable
	private Object getPropertyValue(Object obj) {
		// If a nested property cannot be read, simply return null
		// (similar to JSTL EL). If the property doesn't exist in the
		// first place, let the exception through.
		try {
			BeanWrapperImpl beanWrapper = new BeanWrapperImpl(false);
			beanWrapper.setWrappedInstance(obj);
			return beanWrapper.getPropertyValue(this.sortDefinition.getProperty());
		}
		catch (BeansException ex) {
			logger.debug("PropertyComparator could not access property - treating as null for sorting", ex);
			return null;
		}
	}


	/**
	 * Sort the given List according to the given sort definition.
	 * <p>Note: Contained objects have to provide the given property
	 * in the form of a bean property, i.e. a getXXX method.
	 * @param source the input List
	 * @param sortDefinition the parameters to sort by
	 * @throws java.lang.IllegalArgumentException in case of a missing propertyName
	 */
	public static void sort(List<?> source, SortDefinition sortDefinition) throws BeansException {
		if (StringUtils.hasText(sortDefinition.getProperty())) {
			source.sort(new PropertyComparator<>(sortDefinition));
		}
	}

	/**
	 * Sort the given source according to the given sort definition.
	 * <p>Note: Contained objects have to provide the given property
	 * in the form of a bean property, i.e. a getXXX method.
	 * @param source input source
	 * @param sortDefinition the parameters to sort by
	 * @throws java.lang.IllegalArgumentException in case of a missing propertyName
	 */
	public static void sort(Object[] source, SortDefinition sortDefinition) throws BeansException {
		if (StringUtils.hasText(sortDefinition.getProperty())) {
			Arrays.sort(source, new PropertyComparator<>(sortDefinition));
		}
	}

}
