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
 * A fluent interface for configuring a {@link Mapper} between a source type and a target type.
 * To use, call one or more of the builder methods on this class, then {@link #getMapper()} to obtain the Mapper instance.
 * @author Keith Donald
 * @param <S> the source type to map from
 * @param <T> the target type to map to
 * @see #setAutoMappingEnabled(boolean)
 * @see #addMapping(String)
 * @see #addMapping(String, Converter)
 * @see #addMapping(String, Mapper)
 * @see #addMapping(String, String)
 * @see #addMapping(String, String, Converter)
 * @see #addMapping(Mapper)
 * @see #addConverter(Converter)
 * @see #getMapper()
 */
public interface MapperBuilder<S, T> {

	/**
	 * Sets whether "auto mapping" is enabled.
	 * When enabled, source and target fields with the same name will automatically be mapped unless an explicit mapping override has been registered.
	 * Set to false to require explicit registration of all source-to-target mapping rules.
	 * Default is enabled (true).
	 * @param autoMappingEnabled auto mapping status
	 */
	MapperBuilder<S, T> setAutoMappingEnabled(boolean autoMappingEnabled);

	/**
	 * Register a mapping between a source field and a target field.
	 * The source and target field names will be the same value.
	 * For example, calling <code>addMapping("order")</code> will register a mapping that maps between the <code>order</code> field on the source and the <code>order</code> field on the target.
	 * This is a convenience method for calling {@link #addMapping(String, String)} with the same source and target value..
	 * @param field the field mapping expression
	 * @return this, for configuring additional field mapping options fluently
	 */
	MapperBuilder<S, T> addMapping(String field);

	/**
	 * Register a mapping between a source field and a target field that first converts the source field value using the provided Converter.
	 * The source and target field expressions will be the same value.
	 * For example, calling <code>addMapping("order")</code> will register a mapping that maps between the <code>order</code> field on the source and the <code>order</code> field on the target.
	 * This is a convenience method for calling {@link #addMapping(String, String, Converter)} with the same source and target value..
	 * @param field the field mapping expression
	 * @param converter the converter that will convert the source field value before mapping the value to the target field
	 * @return this, for configuring additional field mapping options fluently
	 */
	MapperBuilder<S, T> addMapping(String field, Converter<?, ?> converter);

	/**
	 * Register a mapping between a source field and multiple target fields.
	 * Use this method when you need to map a single source field value to multiple fields on the target.
	 * For example, calling <code>addMapping("name", firstAndLastNameMapper)</code> might register a mapping that maps the <code>name</code> field on the source to the <code>firstName</code> and <code>lastName</code> fields on the target.
	 * The target field {@link Mapper} will be passed the value of the source field for its source and the target object T for its target.
	 * @param field the source field expression
	 * @param mapper the mapper of the target fields
	 * @return this, for configuring additional field mapping options fluently
	 */
	MapperBuilder<S, T> addMapping(String field, Mapper<?, T> mapper);

	/**
	 * Register a mapping between a source field and a target field.
	 * Use this method when the name of the source field and the name of the target field are different.
	 * For example, calling <code>addMapping("order", "primaryOrder")</code> will register a mapping that maps between the <code>order</code> field on the source and the <code>primaryOrder</code> field on the target.
	 * @param sourceField the source field mapping expression
	 * @param targetField the target field mapping expression 
	 * @return this, for configuring additional field mapping options fluently
	 */
	MapperBuilder<S, T> addMapping(String sourceField, String targetField);

	/**
	 * Register a mapping between a source field and a target field that first converts the source field value using the provided Converter.
	 * Use this method when the name of the source field and the name of the target field are different.
	 * For example, calling <code>addMapping("order", "primaryOrder")</code> will register a mapping that maps between the <code>order</code> field on the source and the <code>primaryOrder</code> field on the target.
	 * @param sourceField the source field mapping expression
	 * @param targetField the target field mapping expression 
	 * @return this, for configuring additional field mapping options fluently
	 */
	MapperBuilder<S, T> addMapping(String sourceField, String targetField, Converter<?, ?> converter);

	/**
	 * Register a mapping between multiple source fields and a single target field.
	 * For example, calling <code>addMapping(dateAndTimeFieldsToDateTimeFieldMapper)</code> might register a mapping that maps the <code>date</code> and <code>time</code> fields on the source to the <code>dateTime</code> field on the target.
	 * The provided {@link Mapper} will be passed the source object S for its source and the target object T for its target.
	 * @param fields the source field mapping expressions
	 * @param mapper the fields to field mapper
	 * @return this, for configuring additional field mapping options fluently
	 */
	MapperBuilder<S, T> addMapping(String[] fields, Mapper<S, T> mapper);

	/**
	 * Register a conditional mapping between a source field and a target field.
	 * The source and target field names will be the same value.
	 * @param field the field mapping expression
	 * @param condition the boolean expression that determines if this mapping executes
	 * @return this, for configuring additional field mapping options fluently
	 */
	MapperBuilder<S, T> addConditionalMapping(String field, String condition);

	/**
	 * Register a condition mapping between a source field and a target field that first converts the source field value using the provided Converter.
	 * The source and target field expressions will be the same value.
	 * @param field the field mapping expression
	 * @param converter the converter that converts the source field value before mapping
	 * @param condition the boolean expression that determines if this mapping executes
	 * @return this, for configuring additional field mapping options fluently
	 */
	MapperBuilder<S, T> addConditionalMapping(String field, Converter<?, ?> converter, String condition);

	/**
	 * Register a conditional mapping between a source field and multiple target fields.
	 * Use this method when you need to map a single source field value to multiple fields on the target.
	 * For example, calling <code>addMapping("name", firstAndLastNameMapper)</code> might register a mapping that maps the <code>name</code> field on the source to the <code>firstName</code> and <code>lastName</code> fields on the target.
	 * The target field {@link Mapper} will be passed the value of the source field for its source and the target object T for its target.
	 * @param field the source field expression
	 * @param mapper the mapper of the target fields
	 * @param condition the boolean expression that determines if this mapping executes
	 * @return this, for configuring additional field mapping options fluently
	 */
	MapperBuilder<S, T> addConditionalMapping(String field, Mapper<?, T> mapper, String condition);

	/**
	 * Register a conditional mapping between a source field and a target field.
	 * Use this method when the name of the source field and the name of the target field are different.
	 * For example, calling <code>addMapping("order", "primaryOrder")</code> will register a mapping that maps between the <code>order</code> field on the source and the <code>primaryOrder</code> field on the target.
	 * @param sourceField the source field mapping expression
	 * @param targetField the target field mapping expression 
	 * @param condition the boolean expression that determines if this mapping executes
	 * @return this, for configuring additional field mapping options fluently
	 */
	MapperBuilder<S, T> addConditionalMapping(String sourceField, String targetField, String condition);

	/**
	 * Register a conditional mapping between a source field and a target field that first converts the source field value using the provided Converter.
	 * Use this method when the name of the source field and the name of the target field are different.
	 * For example, calling <code>addMapping("order", "primaryOrder")</code> will register a mapping that maps between the <code>order</code> field on the source and the <code>primaryOrder</code> field on the target.
	 * @param sourceField the source field mapping expression
	 * @param targetField the target field mapping expression
	 * @param converter the converter that converts the source field value before mapping
	 * @param condition the boolean expression that determines if this mapping executes
	 * @return this, for configuring additional field mapping options fluently
	 */
	MapperBuilder<S, T> addConditionalMapping(String sourceField, String targetField, Converter<?, ?> converter, String condition);

	/**
	 * Register a conditional mapping between multiple source fields and a single target field.
	 * For example, calling <code>addMapping(dateAndTimeFieldsToDateTimeFieldMapper)</code> might register a mapping that maps the <code>date</code> and <code>time</code> fields on the source to the <code>dateTime</code> field on the target.
	 * The provided {@link Mapper} will be passed the source object S for its source and the target object T for its target.
	 * @param fields the source field mapping expressions
	 * @param mapper the fields to field mapper
	 * @param condition the boolean expression that determines if this mapping executes
	 * @return this, for configuring additional field mapping options fluently
	 */
	MapperBuilder<S, T> addMapping(String[] fields, Mapper<S, T> mapper, String condition);
	
	/**
	 * Register a Mapper that will be used to map between nested source and target fields of a specific sourceType/targetType pair.
	 * The source and target field types are determined by introspecting the parameterized types on the Mapper generic interface.
	 * The target instance that is mapped is constructed by calling its default constructor.
	 * @param nestedMapper the nested mapper
	 * @return this, for configuring additional field mapping options fluently
	 */
	MapperBuilder<S, T> addNestedMapper(Mapper<?, ?> nestedMapper);

	/**
	 * Register a Mapper that will be used to map between nested source and target fields of a specific sourceType/targetType pair.
	 * The source and target field types are determined by introspecting the parameterized types on the Mapper generic interface.
	 * The target instance that is mapped is constructed by calling the provided Converter.
	 * @param nestedMapper the nested mapper
	 * @param converter the target converter
	 * @return this, for configuring additional field mapping options fluently
	 */
	MapperBuilder<S, T> addNestedMapper(Mapper<?, ?> nestedMapper, Converter<?, ?> converter);

	/**
	 * Register a custom type converter to use to convert between two mapped types.
	 * The Converter may convert between simple types, such as Strings to Dates.
	 * Alternatively, it may convert between complex types and initiate a recursive mapping operation between two object fields.
	 * @return this, for configuring additional field mapping options fluently
	 * @see Converter
	 * @see MappingConverter
	 */
	MapperBuilder<S, T> addConverter(Converter<?, ?> converter);

	/**
	 * Set the source fields to exclude from mapping.
	 * @param fields the source fields as var args
	 */
	MapperBuilder<S, T>  setExcludedFields(String... fields);

	/**
	 * Get the Mapper produced by this builder.
	 * Call this method after instructing the builder.
	 * @return the Mapper between S and T ready for use
	 */
	Mapper<S, T> getMapper();

}
