/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.validation.annotation;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Validator;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.Errors;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotationMethodValidator implements org.springframework.validation.Validator,
		ApplicationContextAware, InitializingBean {
	private final Map<Class<?>, List<Object>> mappedValidators = new HashMap<Class<?>, List<Object>>();
	private ApplicationContext applicationContext;

	public AnnotationMethodValidator() {
	}

	public void afterPropertiesSet() throws Exception {
		Map<String, Object> beanByName = applicationContext.getBeansWithAnnotation(Validator.class);

		detectValidatedTypes(beanByName.values());
	}

	private void detectValidatedTypes(Collection<Object> beans) {
		for (final Object validatorBean : beans) {
			Validator annotation = AnnotationUtils.findAnnotation(validatorBean.getClass(), Validator.class);
			Class<?> validatedType = annotation.validates();
			addMappedValidator(validatedType, validatorBean);
		}
	}

	private void addMappedValidator(Class<?> validatedType, Object validatorBean) {
		if (mappedValidators.get(validatedType) == null) {
			mappedValidators.put(validatedType, new ArrayList<Object>());
		}
		mappedValidators.get(validatedType).add(validatorBean);
	}

	public boolean supports(Class<?> cls) {
		return mappedValidators.containsKey(cls);
	}

	public void validate(final Object target, final Errors errors) {
		final Class<?> targetType = target.getClass();
		List<Object> validators = mappedValidators.get(targetType);
		if (validators != null) {
			for (final Object validator : validators) {
				ReflectionUtils.doWithMethods(validator.getClass(), new ReflectionUtils.MethodCallback() {
					public void doWith(Method method) {
						Validate validate = AnnotationUtils.findAnnotation(method, Validate.class);
						if (validate != null) {
							invokeValidateMethod(method, validator, target, errors);
						}
					}
				});
			}
		}
	}

	private void invokeValidateMethod(Method method, Object validator, Object target, Errors errors) {
		ReflectionUtils.makeAccessible(method);
		try {
			Object[] params = resolveValidatorParameters(method, validator, target, errors);
			method.invoke(validator, params);
		} catch (IllegalAccessException e) {
			ReflectionUtils.rethrowRuntimeException(e);
		} catch (InvocationTargetException e) {
			ReflectionUtils.rethrowRuntimeException(e.getTargetException());
		}
	}

	private Object[] resolveValidatorParameters(Method method, Object validator, Object target, Errors errors) {
		Class<?>[] parameterTypes = method.getParameterTypes();
		Object[] args = new Object[parameterTypes.length];

		boolean foundErrorsArg = false;

		for (int i = 0; i < parameterTypes.length; i++) {
			Class<?> parameterType = parameterTypes[i];
			if (target.getClass().equals(parameterType)) {
				args[i] = target;
			} else if (Errors.class.isAssignableFrom(parameterType)) {
				args[i] = errors;
				foundErrorsArg = true;
			} else {
				throw new IllegalArgumentException(buildInvalidMethodMessage(method, validator) +
						"Expected method parameter types are " +
						"[" + target.getClass().getName() + "] and " +
						"[" + Errors.class.getName() + "]");
			}
		}

		if (!foundErrorsArg) {
			throw new IllegalArgumentException(buildInvalidMethodMessage(method, validator) +
					"A parameter of type [" + Errors.class.getName() + "] is required.");
		}

		return args;
	}

	private String buildInvalidMethodMessage(Method method, Object object) {
		String methodName = object.getClass().getName() + "." + method.getName();
		return "Validation method [" + methodName + "] has an invalid signature. ";
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
