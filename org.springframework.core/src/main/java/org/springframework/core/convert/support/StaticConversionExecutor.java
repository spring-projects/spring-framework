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

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;

/**
 * Default conversion executor implementation for converters.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
class StaticConversionExecutor implements ConversionExecutor {

	private final TypeDescriptor sourceType;

	private final TypeDescriptor targetType;

	private final Converter converter;


	public StaticConversionExecutor(TypeDescriptor sourceType, TypeDescriptor targetType, Converter converter) {
		this.sourceType = sourceType;
		this.targetType = targetType;
		this.converter = converter;
	}


	@SuppressWarnings("unchecked")
	public Object execute(Object source) throws ConversionFailedException {
		if (source == null) {
			return null;
		}
		if (!this.sourceType.isAssignableValue(source)) {
			throw new ConversionFailedException(source, this.sourceType.getType(), this.targetType.getType(),
					"Source object " + source + " to convert is expected to be an instance of [" + this.sourceType.getName() + "]");
		}
		try {
			return this.converter.convert(source);
		}
		catch (Exception ex) {
			throw new ConversionFailedException(source, this.sourceType.getType(), this.targetType.getType(), ex);
		}
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof StaticConversionExecutor)) {
			return false;
		}
		StaticConversionExecutor other = (StaticConversionExecutor) obj;
		return this.sourceType.equals(other.sourceType) && this.targetType.equals(other.targetType);
	}

	public int hashCode() {
		return this.sourceType.hashCode() + this.targetType.hashCode();
	}

}
