/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.core.convert.service;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterInfo;
import org.springframework.core.convert.converter.SuperConverter;
import org.springframework.core.convert.converter.SuperTwoWayConverter;

/**
 * Adapts a {@link SuperTwoWayConverter} to the {@link Converter} interface in a type safe way. This adapter is useful
 * for applying more general {@link SuperConverter} logic to a specific source/target class pair.
 */
@SuppressWarnings("unchecked")
class SuperTwoWayConverterConverter implements Converter, ConverterInfo {

	private SuperTwoWayConverter superConverter;

	private Class sourceType;

	private Class targetType;

	public SuperTwoWayConverterConverter(SuperTwoWayConverter superConverter, Class sourceType, Class targetType) {
		this.superConverter = superConverter;
		this.sourceType = sourceType;
		this.targetType = targetType;
	}
	
	public Class getSourceType() {
		return sourceType;
	}

	public Class getTargetType() {
		return targetType;
	}

	public Object convert(Object source) throws Exception {
		return superConverter.convert(source, targetType);
	}

	public Object convertBack(Object target) throws Exception {
		return superConverter.convertBack(target, sourceType);
	}

}
