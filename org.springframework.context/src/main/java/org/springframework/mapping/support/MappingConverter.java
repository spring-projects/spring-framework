/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.mapping.support;

import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.GenericConverter;
import org.springframework.mapping.Mapper;

/**
 * Adapts a Mapper to a Converter, allowing the conversion between two object types to be completed by a Mapper.
 * Delegates to a {@link MappingTargetFactory} to construct the conversion target object that will be mapped.
 * The default MapperTargetFactory instantiates a target by calling its default constructor.
 * @author Keith Donald
 */
final class MappingConverter implements GenericConverter {

	private final Mapper mapper;

	private final MappingTargetFactory mappingTargetFactory;

	/**
	 * Creates a new Converter that delegates to the mapper to complete the type conversion process.
	 * Uses the specified MappingTargetFactory to create the target object to map and return.
	 * @param mapper the mapper
	 */
	public MappingConverter(Mapper mapper, MappingTargetFactory mappingTargetFactory) {
		this.mapper = mapper;
		if (mappingTargetFactory != null) {
			this.mappingTargetFactory = mappingTargetFactory;
		} else {
			this.mappingTargetFactory = DefaultMappingTargetFactory.getInstance();
		}
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		if (MappingContextHolder.contains(source)) {
			return source;
		}
		if (sourceType.isAssignableTo(targetType) && isCopyByReference(sourceType, targetType)) {
			return source;
		}
		return createTargetAndMap(source, sourceType, targetType);
	}

	private boolean isCopyByReference(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (BeanUtils.isSimpleValueType(targetType.getType())) {
			return true;
		} else {
			return false;
		}
	}

	private Object createTargetAndMap(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (this.mappingTargetFactory.supports(targetType)) {
			Object target = this.mappingTargetFactory.createTarget(source, sourceType, targetType);
			return this.mapper.map(source, target);
		} else {
			IllegalStateException cause = new IllegalStateException("["
					+ this.mappingTargetFactory.getClass().getName() + "] does not support targetType ["
					+ targetType.getName() + "]");
			throw new ConversionFailedException(sourceType, targetType, source, cause);
		}

	}
}