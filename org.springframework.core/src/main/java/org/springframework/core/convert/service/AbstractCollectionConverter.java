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
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

abstract class AbstractCollectionConverter implements ConversionExecutor {

	private ConversionService conversionService;

	private ConversionExecutor elementConverter;
	
	private TypeDescriptor sourceCollectionType;
	
	private TypeDescriptor targetCollectionType;
	
	public AbstractCollectionConverter(TypeDescriptor sourceCollectionType, TypeDescriptor targetCollectionType, ConversionService conversionService) {
		this.conversionService = conversionService;
		this.sourceCollectionType = sourceCollectionType;
		this.targetCollectionType = targetCollectionType;
		Class<?> sourceElementType = sourceCollectionType.getElementType();
		Class<?> targetElementType = targetCollectionType.getElementType();
		if (sourceElementType != null && targetElementType != null) {
			elementConverter = conversionService.getConversionExecutor(sourceElementType, TypeDescriptor.valueOf(targetElementType));
		} else {
			elementConverter = NoOpConversionExecutor.INSTANCE;
		}
	}

	protected Class<?> getTargetCollectionType() {
		return targetCollectionType.getType();
	}
	
	protected Class<?> getTargetElementType() {
		return targetCollectionType.getElementType();
	}

	protected ConversionService getConversionService() {
		return conversionService;
	}

	protected ConversionExecutor getElementConverter() {
		return elementConverter;
	}

	public Object execute(Object source) throws ConversionExecutionException {
		try {
			return doExecute(source);
		} catch (Exception e) {
			throw new ConversionExecutionException(source, sourceCollectionType.getType(), targetCollectionType, e);
		}
	}

	protected abstract Object doExecute(Object sourceCollection) throws Exception;
	
}