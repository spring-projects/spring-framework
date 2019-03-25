package org.springframework.core.convert.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

import java.util.*;

final class OptionalUnwrappingConverter implements ConditionalGenericConverter {

	private final ConversionService conversionService;

	public OptionalUnwrappingConverter(final ConversionService conversionService)
	{
		this.conversionService = conversionService;
	}

	@Override
	public Set<ConvertiblePair> getConvertibleTypes()
	{
		return null;
	}

	@Override
	public boolean matches(final TypeDescriptor sourceType, final TypeDescriptor targetType)
	{
		if (!Optional.class.isAssignableFrom(sourceType.getObjectType())) {
			return false;
		}

		if (!sourceType.getResolvableType().hasGenerics()) {
			return false;
		}

		return conversionService.canConvert(new GenericTypeDescriptor(sourceType), targetType);
	}

	@Override
	public Object convert(final Object source, final TypeDescriptor sourceType, final TypeDescriptor targetType)
	{
		if (source == null) {
			return null;
		}

		Optional<?> optionalSource = Optional.class.cast(source);
		if (!optionalSource.isPresent()) {
			return null;
		}

		return conversionService.convert(optionalSource.get(), new GenericTypeDescriptor(sourceType), targetType);
	}

	@SuppressWarnings("serial")
	private static class GenericTypeDescriptor extends TypeDescriptor
	{

		GenericTypeDescriptor(final TypeDescriptor typeDescriptor)
		{
			super(typeDescriptor.getResolvableType().getGeneric(), null, typeDescriptor.getAnnotations());
		}

	}

}
