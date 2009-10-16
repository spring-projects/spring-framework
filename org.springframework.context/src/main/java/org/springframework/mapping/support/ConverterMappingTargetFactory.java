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
import org.springframework.core.convert.converter.Converter;

/**
 * Creates a mapping target by calling a converter.
 * @author Keith Donald
 */
final class ConverterMappingTargetFactory implements MappingTargetFactory {

	private Converter converter;

	public ConverterMappingTargetFactory(Converter converter) {
		this.converter = converter;
	}

	public boolean supports(TypeDescriptor targetType) {
		return true;
	}

	public Object createTarget(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		return this.converter.convert(source);
	}

}