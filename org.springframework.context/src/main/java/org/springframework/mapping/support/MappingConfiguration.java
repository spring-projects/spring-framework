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
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.support.GenericConverter;

/**
 * A fluent API for configuring a mapping.
 * @see SpelMapper#addMapping(String)
 * @see SpelMapper#addMapping(String, String)
 * @author Keith Donald
 */
public interface MappingConfiguration {

	/**
	 * Set the type converter to use during this mapping.
	 * @param converter the converter
	 * @return this, for call chaining
	 */
	MappingConfiguration setConverter(Converter<?, ?> converter);

	/**
	 * Set the type converter factory to use during this mapping.
	 * @param converter the converter factory
	 * @return this, for call chaining
	 */
	MappingConfiguration setConverterFactory(ConverterFactory<?, ?> converterFactory);

	/**
	 * Set the generic converter to use during this mapping.
	 * A generic converter allows access to source and target field type descriptors.
	 * These descriptors provide additional context that can be used during type conversion.
	 * @param converter the generic converter
	 * @return this, for call chaining
	 */
	MappingConfiguration setGenericConverter(GenericConverter converter);

	/**
	 * Configures that this mapping should be excluded (ignored and not executed).
	 */
	void setExclude();

}