/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

/**
 * Value object to represent {@link MethodParameters} to allow to easily find the ones
 * with a given annotation.
 * 
 * @author Oliver Gierke
 */
public class MethodParameters {

	private static final ParameterNameDiscoverer DISCOVERER = new LocalVariableTableParameterNameDiscoverer();

	private final List<MethodParameter> parameters;

	/**
	 * Creates a new {@link MethodParameters} from the given {@link Method}.
	 * 
	 * @param method must not be {@literal null}.
	 */
	public MethodParameters(Method method) {
		this(method, null);
	}

	/**
	 * Creates a new {@link MethodParameters} for the given {@link Method} and
	 * {@link AnnotationAttribute}. If the latter is given, method parameter names will be
	 * looked up from the annotation attribute if present.
	 * 
	 * @param method must not be {@literal null}.
	 * @param namingAnnotation can be {@literal null}.
	 */
	public MethodParameters(Method method, AnnotationAttribute namingAnnotation) {

		Assert.notNull(method);
		this.parameters = new ArrayList<MethodParameter>();

		for (int i = 0; i < method.getParameterTypes().length; i++) {

			MethodParameter parameter = new AnnotationNamingMethodParameter(method, i,
					namingAnnotation);
			parameter.initParameterNameDiscovery(DISCOVERER);
			parameters.add(parameter);
		}
	}

	/**
	 * Returns all {@link MethodParameter}s.
	 * 
	 * @return
	 */
	public List<MethodParameter> getParameters() {
		return parameters;
	}

	/**
	 * Returns the {@link MethodParameter} with the given name or {@literal null} if none
	 * found.
	 * 
	 * @param name must not be {@literal null} or empty.
	 * @return
	 */
	public MethodParameter getParameter(String name) {

		Assert.hasText(name, "Parameter name must not be null!");

		for (MethodParameter parameter : parameters) {
			if (name.equals(parameter.getParameterName())) {
				return parameter;
			}
		}

		return null;
	}

	/**
	 * Returns all {@link MethodParameter}s annotated with the given annotation type.
	 * 
	 * @param annotation must not be {@literal null}.
	 * @return
	 */
	public List<MethodParameter> getParametersWith(Class<? extends Annotation> annotation) {

		Assert.notNull(annotation);
		List<MethodParameter> result = new ArrayList<MethodParameter>();

		for (MethodParameter parameter : getParameters()) {
			if (parameter.hasParameterAnnotation(annotation)) {
				result.add(parameter);
			}
		}

		return result;
	}

	/**
	 * Custom {@link MethodParameter} extension that will favor the name configured in the
	 * {@link AnnotationAttribute} if set over discovering it.
	 * 
	 * @author Oliver Gierke
	 */
	private static class AnnotationNamingMethodParameter extends MethodParameter {

		private final AnnotationAttribute attribute;

		private String name;

		/**
		 * Creates a new {@link AnnotationNamingMethodParameter} for the given
		 * {@link Method}'s parameter with the given index.
		 * 
		 * @param method must not be {@literal null}.
		 * @param parameterIndex
		 * @param attribute can be {@literal null}
		 */
		public AnnotationNamingMethodParameter(Method method, int parameterIndex,
				AnnotationAttribute attribute) {

			super(method, parameterIndex);
			this.attribute = attribute;

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.core.MethodParameter#getParameterName()
		 */
		@Override
		public String getParameterName() {

			if (name != null) {
				return name;
			}

			if (attribute != null) {
				Object foundName = attribute.getValueFrom(this);
				if (foundName != null) {
					name = foundName.toString();
					return name;
				}
			}

			name = super.getParameterName();
			return name;
		}
	}
}
