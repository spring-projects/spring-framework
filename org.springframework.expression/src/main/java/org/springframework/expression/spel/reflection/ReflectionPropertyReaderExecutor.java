package org.springframework.expression.spel.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyReaderExecutor;

public class ReflectionPropertyReaderExecutor implements PropertyReaderExecutor {

	private Method methodToAccessProperty;
	private Field fieldToAccessProperty;
	private final String propertyName;

	public ReflectionPropertyReaderExecutor(String propertyName, Method method) {
		this.propertyName = propertyName;
		methodToAccessProperty = method;
	}

	public ReflectionPropertyReaderExecutor(String propertyName, Field field) {
		this.propertyName = propertyName;
		fieldToAccessProperty = field;
	}

	public Object execute(EvaluationContext context, Object target) throws AccessException {
		if (methodToAccessProperty != null) {
			try {
				if (!methodToAccessProperty.isAccessible()) {
					methodToAccessProperty.setAccessible(true);
				}
				return methodToAccessProperty.invoke(target);
			} catch (IllegalArgumentException e) {
				throw new AccessException("Unable to access property '" + propertyName + "' through getter", e);
			} catch (IllegalAccessException e) {
				throw new AccessException("Unable to access property '" + propertyName + "' through getter", e);
			} catch (InvocationTargetException e) {
				throw new AccessException("Unable to access property '" + propertyName + "' through getter", e);
			}
		}
		if (fieldToAccessProperty != null) {
			try {
				if (!fieldToAccessProperty.isAccessible()) {
					fieldToAccessProperty.setAccessible(true);
				}
				return fieldToAccessProperty.get(target);
			} catch (IllegalArgumentException e) {
				throw new AccessException("Unable to access field: " + propertyName, e);
			} catch (IllegalAccessException e) {
				throw new AccessException("Unable to access field: " + propertyName, e);
			}
		}
		throw new AccessException("No method or field accessor found for property '" + propertyName + "'");
	}

}
