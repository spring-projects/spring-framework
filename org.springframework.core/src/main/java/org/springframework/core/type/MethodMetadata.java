package org.springframework.core.type;

import java.util.Map;
import java.util.Set;

public interface MethodMetadata {

	int getModifiers();
	
	boolean isStatic();
	
	String getMethodName();
	
	//TODO does the method return type have a generic wildcard or generic type parameters?
	
	// annotation metadata
	
	Set<String> getAnnotationTypes();
	
	boolean hasAnnotation(String annotationType);
	
	Map<String, Object> getAnnotationAttributes(String annotationType);
	
	/**
	 * Determine whether the underlying class has an annotation that
	 * is itself annotated with the meta-annotation of the given type.
	 * @param metaAnnotationType the meta-annotation type to look for
	 * @return whether a matching meta-annotation is defined
	 */
	boolean hasMetaAnnotation(String metaAnnotationType);
	
	/**
	 * Return the names of all meta-annotation types defined on the
	 * given annotation type of the underlying class.
	 * @return the meta-annotation type names
	 */
	Set<String> getMetaAnnotationTypes(String annotationType);

	Set<String> getAnnotationTypesWithMetaAnnotation(String qualifierClassName);
}
