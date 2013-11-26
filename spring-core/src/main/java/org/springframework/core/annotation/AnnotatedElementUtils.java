/*
 * Copyright 2002-2013 the original author or authors.
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
import java.lang.reflect.AnnotatedElement;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Utility class used to collect all annotation values including those declared on
 * meta-annotations.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @since 4.0
 */
public class AnnotatedElementUtils {

	public static Set<String> getMetaAnnotationTypes(AnnotatedElement element, String annotationType) {
		final Set<String> types = new LinkedHashSet<String>();
		process(element, annotationType, new Processor<Object>() {
			@Override
			public Object process(Annotation annotation, int depth) {
				if (depth > 0) {
					types.add(annotation.annotationType().getName());
				}
				return null;
			}
			@Override
			public void postProcess(Annotation annotation, Object result) {
			}
		});
		return (types.isEmpty() ? null : types);
	}

	public static boolean hasMetaAnnotationTypes(AnnotatedElement element, String annotationType) {
		return Boolean.TRUE.equals(process(element, annotationType, new Processor<Boolean>() {
			@Override
			public Boolean process(Annotation annotation, int depth) {
				if (depth > 0) {
					return true;
				}
				return null;
			}
			@Override
			public void postProcess(Annotation annotation, Boolean result) {
			}
		}));
	}

	public static boolean isAnnotated(AnnotatedElement element, String annotationType) {
		return Boolean.TRUE.equals(process(element, annotationType, new Processor<Boolean>() {
			@Override
			public Boolean process(Annotation annotation, int depth) {
				return true;
			}
			@Override
			public void postProcess(Annotation annotation, Boolean result) {
			}
		}));
	}

	public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement element, String annotationType) {
		return getAnnotationAttributes(element, annotationType, false, false);
	}

	public static AnnotationAttributes getAnnotationAttributes(
			AnnotatedElement element, String annotationType,
			final boolean classValuesAsString, final boolean nestedAnnotationsAsMap) {

		return process(element, annotationType, new Processor<AnnotationAttributes>() {
			@Override
			public AnnotationAttributes process(Annotation annotation, int depth) {
				return AnnotationUtils.getAnnotationAttributes(annotation, classValuesAsString, nestedAnnotationsAsMap);
			}
			@Override
			public void postProcess(Annotation annotation, AnnotationAttributes result) {
				for (String key : result.keySet()) {
					if (!"value".equals(key)) {
						Object value = AnnotationUtils.getValue(annotation, key);
						if (value != null) {
							result.put(key, value);
						}
					}
				}
			}
		});
	}

	public static MultiValueMap<String, Object> getAllAnnotationAttributes(
			AnnotatedElement element, final String annotationType,
			final boolean classValuesAsString, final boolean nestedAnnotationsAsMap) {

		final MultiValueMap<String, Object> attributes = new LinkedMultiValueMap<String, Object>();
		process(element, annotationType, new Processor<Void>() {
			@Override
			public Void process(Annotation annotation, int depth) {
				if (annotation.annotationType().getName().equals(annotationType)) {
					for (Map.Entry<String, Object> entry :
							AnnotationUtils.getAnnotationAttributes(
									annotation, classValuesAsString, nestedAnnotationsAsMap).entrySet()) {
						attributes.add(entry.getKey(), entry.getValue());
					}
				}
				return null;
			}
			@Override
			public void postProcess(Annotation annotation, Void result) {
				for (String key : attributes.keySet()) {
					if (!"value".equals(key)) {
						Object value = AnnotationUtils.getValue(annotation, key);
						if (value != null) {
							attributes.add(key, value);
						}
					}
				}
			}
		});
		return (attributes.isEmpty() ? null : attributes);
	}

	/**
	 * Process all annotations of the specified annotation type and recursively all
	 * meta-annotations on the specified type.
	 * @param element the annotated element
	 * @param annotationType the annotation type to find. Only items of the specified
	 * type or meta-annotations of the specified type will be processed.
	 * @param processor the processor
	 * @return the result of the processor
	 */
	private static <T> T process(AnnotatedElement element, String annotationType, Processor<T> processor) {
		return doProcess(element, annotationType, processor, new HashSet<AnnotatedElement>(), 0);
	}

	private static <T> T doProcess(AnnotatedElement element, String annotationType,
			Processor<T> processor, Set<AnnotatedElement> visited, int depth) {

		if (visited.add(element)) {
			for (Annotation annotation : element.getAnnotations()) {
				if (annotation.annotationType().getName().equals(annotationType) || depth > 0) {
					T result = processor.process(annotation, depth);
					if (result != null)  {
						return result;
					}
					result = doProcess(annotation.annotationType(), annotationType, processor, visited, depth + 1);
					if (result != null) {
						processor.postProcess(annotation, result);
						return result;
					}
				}
			}
			for (Annotation annotation : element.getAnnotations()) {
				T result = doProcess(annotation.annotationType(), annotationType, processor, visited, depth);
				if (result != null) {
					processor.postProcess(annotation, result);
					return result;
				}
			}
		}
		return null;
	}


	/**
	 * Callback interface used to process an annotation.
	 * @param <T> the result type
	 */
	private static interface Processor<T> {

		/**
		 * Called to process the annotation.
		 * @param annotation the annotation to process
		 * @param depth the depth of the annotation relative to the initial match.
		 * For example, a matched annotation will have a depth of 0, a meta-annotation
		 * 1 and a meta-meta-annotation 2
		 * @return the result of the processing or {@code null} to continue
		 */
		T process(Annotation annotation, int depth);

		void postProcess(Annotation annotation, T result);
	}

}
