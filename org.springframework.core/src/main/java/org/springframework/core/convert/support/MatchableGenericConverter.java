package org.springframework.core.convert.support;

import org.springframework.core.convert.TypeDescriptor;

public interface MatchableGenericConverter extends GenericConverter {

	boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType);

}
