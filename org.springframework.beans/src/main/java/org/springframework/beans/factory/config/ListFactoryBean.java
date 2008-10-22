/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.beans.factory.config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.TypeConverter;
import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.JdkVersion;

/**
 * Simple factory for shared List instances. Allows for central setup
 * of Lists via the "list" element in XML bean definitions.
 *
 * @author Juergen Hoeller
 * @since 09.12.2003
 * @see SetFactoryBean
 * @see MapFactoryBean
 */
public class ListFactoryBean extends AbstractFactoryBean {

	private List sourceList;

	private Class targetListClass;


	/**
	 * Set the source List, typically populated via XML "list" elements.
	 */
	public void setSourceList(List sourceList) {
		this.sourceList = sourceList;
	}

	/**
	 * Set the class to use for the target List. Can be populated with a fully
	 * qualified class name when defined in a Spring application context.
	 * <p>Default is a <code>java.util.ArrayList</code>.
	 * @see java.util.ArrayList
	 */
	public void setTargetListClass(Class targetListClass) {
		if (targetListClass == null) {
			throw new IllegalArgumentException("'targetListClass' must not be null");
		}
		if (!List.class.isAssignableFrom(targetListClass)) {
			throw new IllegalArgumentException("'targetListClass' must implement [java.util.List]");
		}
		this.targetListClass = targetListClass;
	}


	public Class getObjectType() {
		return List.class;
	}

	protected Object createInstance() {
		if (this.sourceList == null) {
			throw new IllegalArgumentException("'sourceList' is required");
		}
		List result = null;
		if (this.targetListClass != null) {
			result = (List) BeanUtils.instantiateClass(this.targetListClass);
		}
		else {
			result = new ArrayList(this.sourceList.size());
		}
		Class valueType = null;
		if (this.targetListClass != null && JdkVersion.isAtLeastJava15()) {
			valueType = GenericCollectionTypeResolver.getCollectionType(this.targetListClass);
		}
		if (valueType != null) {
			TypeConverter converter = getBeanTypeConverter();
			for (Iterator it = this.sourceList.iterator(); it.hasNext();) {
				result.add(converter.convertIfNecessary(it.next(), valueType));
			}
		}
		else {
			result.addAll(this.sourceList);
		}
		return result;
	}

}
