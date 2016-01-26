/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.core.MethodParameter;

/**
 * A {@link MethodParameter} variant which synthesizes annotations that
 * declare attribute aliases via {@link AliasFor @AliasFor}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.2
 * @see AnnotationUtils#synthesizeAnnotation
 * @see AnnotationUtils#synthesizeAnnotationArray
 */
public class SynthesizingMethodParameter extends MethodParameter {

	/**
	 * Create a new {@code SynthesizingMethodParameter} for the given method.
	 * @param method the Method to specify a parameter for
	 * @param parameterIndex the index of the parameter: -1 for the method
	 * return type; 0 for the first method parameter; 1 for the second method
	 * parameter, etc.
	 */
	public SynthesizingMethodParameter(Method method, int parameterIndex) {
		super(method, parameterIndex);
	}

	protected SynthesizingMethodParameter(SynthesizingMethodParameter original) {
		super(original);
	}


	@Override
	protected <A extends Annotation> A adaptAnnotation(A annotation) {
		return AnnotationUtils.synthesizeAnnotation(annotation, getAnnotatedElement());
	}

	@Override
	protected Annotation[] adaptAnnotationArray(Annotation[] annotations) {
		return AnnotationUtils.synthesizeAnnotationArray(annotations, getAnnotatedElement());
	}


	@Override
	public SynthesizingMethodParameter clone() {
		return new SynthesizingMethodParameter(this);
	}

}
