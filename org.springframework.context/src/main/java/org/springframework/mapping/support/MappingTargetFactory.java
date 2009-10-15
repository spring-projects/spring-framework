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

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.mapping.Mapper;

/**
 * A factory for customizing how the target of a map operation is constructed.
 * Used by a {@link MappingConverter} when executing a type conversion.
 * @author Keith Donald
 * @see MappingConverter
 * @see Mapper#map(Object, Object)
 */
public interface MappingTargetFactory {

	/**
	 * Does this factory support creating mapping targets of the specified type
	 * @param targetType the target type
	 * @return true if so, false otherwise
	 */
	public boolean supports(TypeDescriptor targetType);

	/**
	 * Create the target object to be mapped to.
	 * @param source the source object to map from
	 * @param sourceType the source object type descriptor
	 * @param targetType the target object type descriptor
	 * @return the target
	 */
	public Object createTarget(TypeDescriptor targetType);

}