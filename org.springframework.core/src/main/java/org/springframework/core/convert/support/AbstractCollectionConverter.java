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

/**
 * Base class for converters that convert to and from collection types (arrays and java.util.Collection types).
 *
 * @author Keith Donald
 * @since 3.0
 */
abstract class AbstractCollectionConverter implements ConversionExecutor {

	private final GenericConversionService conversionService;

	private final TypeDescriptor sourceCollectionType;

	private final TypeDescriptor targetCollectionType;

	private final ConversionExecutor elementConverter;


	public AbstractCollectionConverter(TypeDescriptor sourceCollectionType, TypeDescriptor targetCollectionType, GenericConversionService conversionService) {
		this.conversionService = conversionService;
		this.sourceCollectionType = sourceCollectionType;
		this.targetCollectionType = targetCollectionType;
		Class<?> sourceElementType = sourceCollectionType.getElementType();
		Class<?> targetElementType = targetCollectionType.getElementType();
		if (sourceElementType != null && targetElementType != null) {
			ConversionExecutor executor = conversionService.getConversionExecutor(sourceElementType, TypeDescriptor.valueOf(targetElementType));
			if (executor != null) {
				this.elementConverter = executor;
			}
			else {
				this.elementConverter = NoOpConversionExecutor.INSTANCE;
			}
		}
		else {
			this.elementConverter = NoOpConversionExecutor.INSTANCE;
		}
	}


	/**
	 * The collection type to convert to.
	 */
	protected Class<?> getTargetCollectionType() {
		return targetCollectionType.getType();
	}

	/**
	 * The type of elements in the target collection.
	 */
	protected Class<?> getTargetElementType() {
		return this.targetCollectionType.getElementType();
	}

	protected GenericConversionService getConversionService() {
		return this.conversionService;
	}

	/**
	 * The converter to use to convert elements to the {@link #getTargetElementType()}.
	 * Returns {@link NoOpConversionExecutor#INSTANCE} if no converter could be eagerly resolved from type descriptor metadata.
	 */
	protected ConversionExecutor getElementConverter() {
		return this.elementConverter;
	}


	public Object execute(Object source) {
		try {
			return doExecute(source);
		}
		catch (Exception ex) {
			throw new ConversionFailedException(source, this.sourceCollectionType.getType(), this.targetCollectionType.getType(), ex);
		}
	}

	/**
	 * Override to perform collection conversion.
	 * @param sourceCollection the source collection to convert from, an instance of sourceCollectionType,
	 * which must be either an array or java.util.Collection type.
	 * @return the converted target collection, an instance of targetCollectionType
	 * @throws Exception an exception occurred during the conversion
	 */
	protected abstract Object doExecute(Object sourceCollection) throws Exception;
	
}