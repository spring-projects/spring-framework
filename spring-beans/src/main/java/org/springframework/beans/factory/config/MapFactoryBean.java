/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.config;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.TypeConverter;
import org.springframework.core.ResolvableType;
import org.springframework.util.CollectionUtils;

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

	private @Nullable Map<?, ?> sourceMap;

	@SuppressWarnings("rawtypes")
	private @Nullable Class<? extends Map> targetMapClass;


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
			result = CollectionUtils.newLinkedHashMap(this.sourceMap.size());
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
