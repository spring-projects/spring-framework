package org.springframework.core.convert.converter;

public interface ConverterInfo {

	public Class<?> getSourceType();
	
	public Class<?> getTargetType();

}
