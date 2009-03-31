/*
 * Copyright 2004-2009 the original author or authors.
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
import org.springframework.core.convert.TypeDescriptor;

abstract class AbstractCollectionConverter implements ConversionExecutor {

	private TypeDescriptor sourceCollectionType;
	
	private TypeDescriptor targetCollectionType;
	
	private GenericConversionService conversionService;

	private ConversionExecutor elementConverter;
	
	public AbstractCollectionConverter(TypeDescriptor sourceCollectionType, TypeDescriptor targetCollectionType, GenericConversionService conversionService) {
		this.sourceCollectionType = sourceCollectionType;
		this.targetCollectionType = targetCollectionType;
		this.conversionService = conversionService;
		this.elementConverter = createElementConverter();
	}
	
	private ConversionExecutor createElementConverter() {
		Class<?> sourceElementType = getSourceType().getElementType();
		Class<?> targetElementType = getTargetType().getElementType();
		return (sourceElementType != null && targetElementType != null) ? conversionService.getElementConverter(sourceElementType, targetElementType) : null;
	}
	
	protected TypeDescriptor getSourceType() {
		return sourceCollectionType;
	}

	protected TypeDescriptor getTargetType() {
		return targetCollectionType;
	}

	protected GenericConversionService getConversionService() {
		return conversionService;
	}

	protected ConversionExecutor getElementConverter() {
		return elementConverter;
	}

	public Object execute(Object source) throws ConversionExecutionException {
		try {
			return doExecute(source);
		} catch (Exception e) {
			throw new ConversionExecutionException(source, sourceCollectionType, targetCollectionType, e);
		}
	}

	protected abstract Object doExecute(Object sourceCollection) throws Exception;
	
}