package org.springframework.mapping.support;

import java.util.Set;

import org.springframework.expression.EvaluationContext;

interface MappableType {
	
	Set<String> getMappableFields(Object instance);
	
	EvaluationContext getMappingContext(Object instance);
	
}