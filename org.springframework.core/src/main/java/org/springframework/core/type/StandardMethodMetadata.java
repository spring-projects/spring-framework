package org.springframework.core.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.annotation.AnnotationUtils;

public class StandardMethodMetadata implements MethodMetadata {

	private final Method introspectedMethod;
	
	public StandardMethodMetadata(Method method) {
		introspectedMethod = method;
	}
	
	public final Method getIntrospectedMethod() {
		return this.introspectedMethod;
	}
	

	public Map<String, Object> getAnnotationAttributes(String annotationType) {
		Annotation[] anns = getIntrospectedMethod().getAnnotations();
		for (int i = 0; i < anns.length; i++) {
			Annotation ann = anns[i];
			if (ann.annotationType().getName().equals(annotationType)) {
				return AnnotationUtils.getAnnotationAttributes(ann);
			}
		}
		return null;
	}

	public Set<String> getAnnotationTypes() {
		Set<String> types = new HashSet<String>();
		Annotation[] anns = getIntrospectedMethod().getAnnotations();
		for (int i = 0; i < anns.length; i++) {
			types.add(anns[i].annotationType().getName());
		}
		return types;
	}
	
	


	public String getMethodName() {
		return introspectedMethod.getName();
	}

	public int getModifiers() {
		return introspectedMethod.getModifiers();
	}

	public boolean hasAnnotation(String annotationType) {
		Annotation[] anns = getIntrospectedMethod().getAnnotations();
		for (int i = 0; i < anns.length; i++) {
			if (anns[i].annotationType().getName().equals(annotationType)) {
				return true;
			}
		}
		return false;
	}

	public boolean isStatic() {
		return Modifier.isStatic(getIntrospectedMethod().getModifiers());
	}

	public Set<String> getMetaAnnotationTypes(String annotationType) {
		Annotation[] anns = getIntrospectedMethod().getAnnotations();
		for (int i = 0; i < anns.length; i++) {
			if (anns[i].annotationType().getName().equals(annotationType)) {
				Set<String> types = new HashSet<String>();
				Annotation[] metaAnns = anns[i].annotationType().getAnnotations();
				for (Annotation meta : metaAnns) {
					types.add(meta.annotationType().getName());
				}
				return types;
			}
		}
		return null;
	}


	public boolean hasMetaAnnotation(String metaAnnotationType) {
		
		//TODO can refactor into shared (utility) method with StandardAnnotationMetadata
		Annotation[] anns = getIntrospectedMethod().getAnnotations();
		for (int i = 0; i < anns.length; i++) {
			Annotation[] metaAnns = anns[i].annotationType().getAnnotations();
			for (Annotation meta : metaAnns) {
				if (meta.annotationType().getName().equals(metaAnnotationType)) {
					return true;
				}
			}
		}
		return false;
	}

	public Set<String> getAnnotationTypesWithMetaAnnotation(
			String qualifierClassName) {
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
