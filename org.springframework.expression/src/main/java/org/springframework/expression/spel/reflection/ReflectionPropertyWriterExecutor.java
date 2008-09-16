package org.springframework.expression.spel.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyWriterExecutor;

public class ReflectionPropertyWriterExecutor implements PropertyWriterExecutor {

	private Method methodToAccessProperty;
	private Field fieldToAccessProperty;
	private final String propertyName;

	public ReflectionPropertyWriterExecutor(String propertyName, Method method) {
		this.propertyName = propertyName;
		methodToAccessProperty = method;
	}

	public ReflectionPropertyWriterExecutor(String propertyName, Field field) {
		this.propertyName = propertyName;
		fieldToAccessProperty = field;
	}

	// public Object execute(EvaluationContext context, Object target) throws AccessException {
	public void execute(EvaluationContext evaluationContext, Object target, Object newValue) throws AccessException {
		if (methodToAccessProperty != null) {
			try {
				if (!methodToAccessProperty.isAccessible())
					methodToAccessProperty.setAccessible(true);
				methodToAccessProperty.invoke(target, newValue);
				return;
			} catch (IllegalArgumentException e) {
				throw new AccessException("Unable to access property '" + propertyName + "' through setter", e);
			} catch (IllegalAccessException e) {
				throw new AccessException("Unable to access property '" + propertyName + "' through setter", e);
			} catch (InvocationTargetException e) {
				throw new AccessException("Unable to access property '" + propertyName + "' through setter", e);
			}
		}
		if (fieldToAccessProperty != null) {
			try {
				if (!fieldToAccessProperty.isAccessible()) {
					fieldToAccessProperty.setAccessible(true);
				}
				fieldToAccessProperty.set(target, newValue);
				return;
			} catch (IllegalArgumentException e) {
				throw new AccessException("Unable to access field: " + propertyName, e);
			} catch (IllegalAccessException e) {
				throw new AccessException("Unable to access field: " + propertyName, e);
			}
		}
		throw new AccessException("No method or field accessor found for property '" + propertyName + "'");
	}
}
