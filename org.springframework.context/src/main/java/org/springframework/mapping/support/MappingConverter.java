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
public final class MappingConverter implements GenericConverter {

	private Mapper mapper;

	private MappingTargetFactory mappingTargetFactory;

	public MappingConverter(Mapper mapper) {
		this(mapper, new DefaultMappingTargetFactory());
	}

	public MappingConverter(Mapper mapper, MappingTargetFactory mappingTargetFactory) {
		this.mapper = mapper;
		this.mappingTargetFactory = mappingTargetFactory;
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
		return createAndMap(targetType, source, sourceType);
	}

	private boolean isCopyByReference(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (BeanUtils.isSimpleValueType(targetType.getType())) {
			return true;
		} else {
			return false;
		}
	}

	private Object createAndMap(TypeDescriptor targetType, Object source, TypeDescriptor sourceType) {
		if (this.mappingTargetFactory.supports(targetType)) {
			Object target = this.mappingTargetFactory.createTarget(targetType);
			return this.mapper.map(source, target);
		} else {
			IllegalStateException cause = new IllegalStateException("["
					+ this.mappingTargetFactory.getClass().getName() + "] does not support target type ["
					+ targetType.getName() + "]");
			throw new ConversionFailedException(sourceType, targetType, source, cause);
		}

	}
}