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
package org.springframework.core.convert.support;

import java.util.Collection;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;

/**
 * Converts an object to a single-element collection.
 * @author Keith Donald
 * @since 3.0
 */
@SuppressWarnings("unchecked")
class ObjectToCollection implements ConversionExecutor {

	private TypeDescriptor sourceObjectType;
	
	private TypeDescriptor targetCollectionType;

	private GenericTypeConverter typeConverter;
	
	private ConversionExecutor elementConverter;
	
	public ObjectToCollection(TypeDescriptor sourceObjectType, TypeDescriptor targetCollectionType,
			GenericTypeConverter typeConverter) {
		this.sourceObjectType = sourceObjectType;
		this.targetCollectionType = targetCollectionType;
		this.typeConverter = typeConverter;
		initElementConverter();
	}

	public Object execute(Object source) throws ConversionFailedException {
		Class implClass = ConversionUtils.getCollectionImpl(targetCollectionType.getType());
		Collection collection;
		try {
			collection = (Collection) implClass.newInstance();
		} catch (InstantiationException e) {
			throw new ConversionFailedException(source, sourceObjectType.getType(), targetCollectionType.getType(), e);
		} catch (IllegalAccessException e) {
			throw new ConversionFailedException(source, sourceObjectType.getType(), targetCollectionType.getType(), e);
		}
		collection.add(elementConverter.execute(source));
		return collection;
	}

	private void initElementConverter() {
		Class<?> elementType = targetCollectionType.getElementType();
		if (elementType != null) { 
			this.elementConverter = typeConverter.getConversionExecutor(sourceObjectType.getType(), TypeDescriptor.valueOf(elementType));
		} else {
			this.elementConverter = NoOpConversionExecutor.INSTANCE;
		}
	}
}