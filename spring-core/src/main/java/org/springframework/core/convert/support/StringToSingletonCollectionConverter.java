package org.springframework.core.convert.support;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Converts a String to a Singleton Collection.
 * If the target collection element type is declared, only matches if
 * {@code String.class} can be converted to it.
 *
 * @author Dmytro Zhelieznyi
 * @since 5.0
 */
public final class StringToSingletonCollectionConverter implements ConditionalGenericConverter {

	private final ConversionService conversionService;


	public StringToSingletonCollectionConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new GenericConverter.ConvertiblePair(String.class, Collection.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return (targetType.getElementTypeDescriptor() == null ||
				this.conversionService.canConvert(sourceType, targetType.getElementTypeDescriptor()));
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		String string = (String) source;
		String[] fields = {string};

		TypeDescriptor elementDesc = targetType.getElementTypeDescriptor();
		Collection<Object> target = CollectionFactory.createCollection(targetType.getType(),
				(elementDesc != null ? elementDesc.getType() : null), fields.length);

		if (elementDesc == null) {
			for (String field : fields) {
				target.add(field.trim());
			}
		} else {
			for (String field : fields) {
				Object targetElement = this.conversionService.convert(field.trim(), sourceType, elementDesc);
				target.add(targetElement);
			}
		}
		return target;
	}

}
