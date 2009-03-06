/*
 * Copyright 2004-2008 the original author or authors.
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
package org.springframework.core.convert.service;

import org.springframework.core.convert.ConversionExecutionException;
import org.springframework.core.convert.ConversionExecutor;
import org.springframework.core.convert.converter.SuperConverter;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;

@SuppressWarnings("unchecked")
class StaticSuperConversionExecutor implements ConversionExecutor {

	private final Class sourceClass;

	private final Class targetClass;

	private final SuperConverter converter;

	public StaticSuperConversionExecutor(Class sourceClass, Class targetClass, SuperConverter converter) {
		Assert.notNull(sourceClass, "The source class is required");
		Assert.notNull(targetClass, "The target class is required");
		Assert.notNull(converter, "The super converter is required");
		this.sourceClass = sourceClass;
		this.targetClass = targetClass;
		this.converter = converter;
	}

	public Class getSourceClass() {
		return sourceClass;
	}
	
	public Class getTargetClass() {
		return targetClass;
	}

	public Object execute(Object source) throws ConversionExecutionException {
		if (source == null) {
			return null;
		}
		if (!sourceClass.isInstance(source)) {
			throw new ConversionExecutionException(source, getSourceClass(), getTargetClass(), "Source object "
					+ source + " to convert is expected to be an instance of [" + getSourceClass().getName() + "]");
		}
		try {
			return converter.convert(source, targetClass);
		} catch (Exception e) {
			throw new ConversionExecutionException(source, getSourceClass(), getTargetClass(), e);
		}
	}

	public boolean equals(Object o) {
		if (!(o instanceof StaticSuperConversionExecutor)) {
			return false;
		}
		StaticSuperConversionExecutor other = (StaticSuperConversionExecutor) o;
		return sourceClass.equals(other.sourceClass) && targetClass.equals(other.targetClass);
	}

	public int hashCode() {
		return sourceClass.hashCode() + targetClass.hashCode();
	}

	public String toString() {
		return new ToStringCreator(this).append("sourceClass", sourceClass).append("targetClass", targetClass)
				.toString();
	}
}