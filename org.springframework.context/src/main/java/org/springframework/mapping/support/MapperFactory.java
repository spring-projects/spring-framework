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

import org.springframework.mapping.Mapper;

/**
 * Factory for creating general-purpose Mappers without depending on a concrete Mapper implementation class.
 * @see #defaultMapper()
 * @see #mapperBuilder()
 * @see #mapperBuilder(Class, Class)
 * @author Keith Donald
 */
public class MapperFactory {

	private static final SpelMapper DEFAULT_MAPPER = new SpelMapper();

	/**
	 * Get the default Mapper instance suitable for mapping between most object types using "auto mapping" based on field names.
	 * The Mapper returned is shared and immutable and should not be downcast & modified.
	 * @return the default mapper
	 */
	public static Mapper<Object, Object> defaultMapper() {
		return DEFAULT_MAPPER;
	}

	/**
	 * Get a builder for a new Mapper instance, allowing customization of object mapping policy.
	 * @return the MapperBuilder
	 */
	public static MapperBuilder<Object, Object> mapperBuilder() {
		return new SpelMapperBuilder<Object, Object>();
	}

	/**
	 * Get a builder for a new Mapper instance that maps between objects of sourceType and targetType.
	 * Allows for customization of object mapping policy.
	 * Use this method as an alterntative to {@link #mapperBuilder()} when you'd like more type-safety and validation when configuring and using the Mapper.
	 * @return the MapperBuilder
	 */
	public static <S, T> MapperBuilder<S, T> mapperBuilder(Class<S> sourceType, Class<T> targetType) {
		return new SpelMapperBuilder<S, T>(sourceType, targetType);
	}

}
