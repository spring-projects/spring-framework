/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.annotation;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;

import org.jspecify.annotations.Nullable;

/**
 * Adapter for exposing a set of annotations as an {@link AnnotatedElement}, in
 * particular as input for various methods in {@link AnnotatedElementUtils}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 7.0
 * @see #from(Annotation[])
 * @see AnnotatedElementUtils#isAnnotated(AnnotatedElement, Class)
 * @see AnnotatedElementUtils#getMergedAnnotation(AnnotatedElement, Class)
 */
@SuppressWarnings("serial")
public final class AnnotatedElementAdapter implements AnnotatedElement, Serializable {

	private static final AnnotatedElementAdapter EMPTY = new AnnotatedElementAdapter(new Annotation[0]);


	/**
	 * Create an {@code AnnotatedElementAdapter} from the supplied annotations.
	 * <p>The supplied annotations will be considered to be both <em>present</em>
	 * and <em>directly present</em> with regard to the results returned from
	 * methods such as {@link #getAnnotation(Class)},
	 * {@link #getDeclaredAnnotation(Class)}, etc.
	 * <p>If the supplied annotations array is either {@code null} or empty, this
	 * factory method will return an {@linkplain #isEmpty() empty} adapter.
	 * @param annotations the annotations to expose via the {@link AnnotatedElement}
	 * API
	 * @return a new {@code AnnotatedElementAdapter}
	 */
	public static AnnotatedElementAdapter from(Annotation @Nullable [] annotations) {
		if (annotations == null || annotations.length == 0) {
			return EMPTY;
		}
		return new AnnotatedElementAdapter(annotations);
	}


	private final Annotation[] annotations;


	private AnnotatedElementAdapter(Annotation[] annotations) {
		this.annotations = annotations;
	}


	@Override
	public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
		for (Annotation annotation : this.annotations) {
			if (annotation.annotationType() == annotationClass) {
				return true;
			}
		}
		return false;
	}

	@Override
	public <A extends Annotation> @Nullable A getAnnotation(Class<A> annotationClass) {
		for (Annotation annotation : this.annotations) {
			if (annotation.annotationType() == annotationClass) {
				return annotationClass.cast(annotation);
			}
		}
		return null;
	}

	@Override
	public Annotation[] getAnnotations() {
		return (isEmpty() ? this.annotations : this.annotations.clone());
	}

	@Override
	public <A extends Annotation> @Nullable A getDeclaredAnnotation(Class<A> annotationClass) {
		return getAnnotation(annotationClass);
	}

	@Override
	public Annotation[] getDeclaredAnnotations() {
		return getAnnotations();
	}

	/**
	 * Determine if this {@code AnnotatedElementAdapter} is empty.
	 * @return {@code true} if this adapter contains no annotations
	 */
	public boolean isEmpty() {
		return (this == EMPTY);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof AnnotatedElementAdapter that &&
				Arrays.equals(this.annotations, that.annotations)));
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.annotations);
	}

	@Override
	public String toString() {
		return Arrays.toString(this.annotations);
	}

}
