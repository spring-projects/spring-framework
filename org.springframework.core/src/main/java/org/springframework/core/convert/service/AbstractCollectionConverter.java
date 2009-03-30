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

	@Override
	public Object execute(Object source) throws ConversionExecutionException {
		try {
			return doExecute(source);
		} catch (Exception e) {
			throw new ConversionExecutionException(source, sourceCollectionType, targetCollectionType, e);
		}
	}

	protected abstract Object doExecute(Object sourceCollection) throws Exception;
	
}