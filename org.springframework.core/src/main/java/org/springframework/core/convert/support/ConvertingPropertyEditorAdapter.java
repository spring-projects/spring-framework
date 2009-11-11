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

package org.springframework.core.convert.support;

import java.beans.PropertyEditorSupport;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.Assert;

/**
 * Adapter that exposes a {@link java.beans.PropertyEditor} for any given
 * {@link org.springframework.core.convert.ConversionService} and specific target type.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ConvertingPropertyEditorAdapter extends PropertyEditorSupport {

	private static final TypeDescriptor stringDescriptor = TypeDescriptor.valueOf(String.class);

	private final ConversionService conversionService;

	private final TypeDescriptor targetDescriptor;


	/**
	 * Create a new ConvertingPropertyEditorAdapter for a given
	 * {@link org.springframework.core.convert.ConversionService}
	 * and the given target type.
	 * @param conversionService the ConversionService to delegate to
	 * @param targetDescriptor the target type to convert to
	 */
	public ConvertingPropertyEditorAdapter(ConversionService conversionService, TypeDescriptor targetDescriptor) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		Assert.notNull(targetDescriptor, "TypeDescriptor must not be null");
		this.conversionService = conversionService;
		this.targetDescriptor = targetDescriptor;
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(this.conversionService.convert(text, stringDescriptor, this.targetDescriptor));
	}

	@Override
	public String getAsText() {
		return (String) this.conversionService.convert(getValue(), this.targetDescriptor, stringDescriptor);
	}

}
