/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.beans.factory.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.TypeConverter;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * Simple factory for shared Map instances. Allows for central setup
 * of Maps via the "map" element in XML bean definitions.
 *
 * @author Juergen Hoeller
 * @since 09.12.2003
 * @see SetFactoryBean
 * @see ListFactoryBean
 */
public class MapFactoryBean extends AbstractFactoryBean<Map<Object, Object>> {

	@Nullable
	private Map<?, ?> sourceMap;

	@SuppressWarnings("rawtypes")
	@Nullable
	private Class<? extends Map> targetMapClass;


	/**
	 * Set the source Map, typically populated via XML "map" elements.
	 */
	public void setSourceMap(Map<?, ?> sourceMap) {
		this.sourceMap = sourceMap;
	}

	/**
	 * Set the class to use for the target Map. Can be populated with a fully
	 * qualified class name when defined in a Spring application context.
	 * <p>Default is a linked HashMap, keeping the registration order.
	 * @see java.util.LinkedHashMap
	 */
	@SuppressWarnings("rawtypes")
	public void setTargetMapClass(@Nullable Class<? extends Map> targetMapClass) {
		if (targetMapClass == null) {
			throw new IllegalArgumentException("'targetMapClass' must not be null");
		}
		if (!Map.class.isAssignableFrom(targetMapClass)) {
			throw new IllegalArgumentException("'targetMapClass' must implement [java.util.Map]");
		}
		this.targetMapClass = targetMapClass;
	}


	@Override
	@SuppressWarnings("rawtypes")
	public Class<Map> getObjectType() {
		return Map.class;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Map<Object, Object> createInstance() {
		if (this.sourceMap == null) {
			throw new IllegalArgumentException("'sourceMap' is required");
		}
		Map<Object, Object> result = null;
		if (this.targetMapClass != null) {
			result = BeanUtils.instantiateClass(this.targetMapClass);
		}
		else {
			result = new LinkedHashMap<>(this.sourceMap.size());
		}
		Class<?> keyType = null;
		Class<?> valueType = null;
		if (this.targetMapClass != null) {
			ResolvableType mapType = ResolvableType.forClass(this.targetMapClass).asMap();
			keyType = mapType.resolveGeneric(0);
			valueType = mapType.resolveGeneric(1);
		}
		if (keyType != null || valueType != null) {
			TypeConverter converter = getBeanTypeConverter();
			for (Map.Entry<?, ?> entry : this.sourceMap.entrySet()) {
				Object convertedKey = converter.convertIfNecessary(entry.getKey(), keyType);
				Object convertedValue = converter.convertIfNecessary(entry.getValue(), valueType);
				result.put(convertedKey, convertedValue);
			}
		}
		else {
			result.putAll(this.sourceMap);
		}
		return result;
	}

}
