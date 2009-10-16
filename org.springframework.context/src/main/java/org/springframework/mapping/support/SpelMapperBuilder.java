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

import org.springframework.core.convert.converter.Converter;
import org.springframework.mapping.Mapper;

/**
 * MapperBuilder that builds {@link SpelMapper} instances.
 * @author Keith Donald
 */
final class SpelMapperBuilder<S, T> implements MapperBuilder<S, T> {

	private final SpelMapper mapper;

	public SpelMapperBuilder() {
		this.mapper = new SpelMapper();
	}

	public SpelMapperBuilder(Class<S> sourceType, Class<T> targetType) {
		this.mapper = new SpelMapper(sourceType, targetType);
	}

	public MapperBuilder<S, T> setAutoMappingEnabled(boolean autoMappingEnabled) {
		this.mapper.setAutoMappingEnabled(autoMappingEnabled);
		return this;
	}

	public MapperBuilder<S, T> addMapping(String field) {
		this.mapper.addMapping(field, field, null);
		return this;
	}

	public MapperBuilder<S, T> addMapping(String field, Converter<?, ?> converter) {
		this.mapper.addMapping(field, field, converter);
		return this;
	}

	public MapperBuilder<S, T> addMapping(String field, Mapper<?, T> mapper) {
		this.mapper.addMapping(field, mapper);
		return this;
	}

	public MapperBuilder<S, T> addMapping(String sourceField, String targetField) {
		this.mapper.addMapping(sourceField, targetField, null);
		return this;
	}

	public MapperBuilder<S, T> addMapping(String sourceField, String targetField, Converter<?, ?> converter) {
		this.mapper.addMapping(sourceField, targetField, converter);
		return this;
	}

	public MapperBuilder<S, T> addMapping(Mapper<S, T> mapper) {
		this.mapper.addMapping(mapper);
		return this;
	}

	public MapperBuilder<S, T> addNestedMapper(Mapper<?, ?> nestedMapper) {
		this.mapper.addNestedMapper(nestedMapper);
		return this;
	}

	public MapperBuilder<S, T> addNestedMapper(Mapper<?, ?> nestedMapper, Converter<?, ?> converter) {
		this.mapper.addNestedMapper(nestedMapper, new ConverterMappingTargetFactory(converter));
		return this;
	}

	public MapperBuilder<S, T> addConverter(Converter<?, ?> converter) {
		this.mapper.getConverterRegistry().addConverter(converter);
		return this;
	}

	@SuppressWarnings("unchecked")
	public Mapper<S, T> getMapper() {
		return (Mapper<S, T>) this.mapper;
	}

}
