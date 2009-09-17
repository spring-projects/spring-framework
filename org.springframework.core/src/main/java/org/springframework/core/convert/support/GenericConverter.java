package org.springframework.core.convert.support;

import org.springframework.core.convert.TypeDescriptor;

public interface GenericConverter {
	
	Object convert(Object source, TypeDescriptor type);
	
}
