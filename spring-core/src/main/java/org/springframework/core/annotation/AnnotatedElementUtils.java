/*
 * Copyright 2002-2014 the original author or authors.
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

import static org.springframework.core.annotation.AnnotationUtils.*;

/**
 * Utility class used to collect all annotation values including those declared on
 * meta-annotations.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
public class AnnotatedElementUtils {

	public static Set<String> getMetaAnnotationTypes(AnnotatedElement element, String annotationType) {
		final Set<String> types = new LinkedHashSet<String>();
		process(element, annotationType, true, new Processor<Object>() {
			@Override
			public Object process(Annotation annotation, int metaDepth) {
				if (metaDepth > 0) {
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
		return Boolean.TRUE.equals(process(element, annotationType, true, new Processor<Boolean>() {
			@Override
			public Boolean process(Annotation annotation, int metaDepth) {
				if (metaDepth > 0) {
					return Boolean.TRUE;
				}
				return null;
			}
			@Override
			public void postProcess(Annotation annotation, Boolean result) {
			}
		}));
	}

	public static boolean isAnnotated(AnnotatedElement element, String annotationType) {
		return Boolean.TRUE.equals(process(element, annotationType, true, new Processor<Boolean>() {
			@Override
			public Boolean process(Annotation annotation, int metaDepth) {
				return Boolean.TRUE;
			}
			@Override
			public void postProcess(Annotation annotation, Boolean result) {
			}
		}));
	}

	public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement element, String annotationType) {
		return getAnnotationAttributes(element, annotationType, false, false);
	}

	public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement element, String annotationType,
			final boolean classValuesAsString, final boolean nestedAnnotationsAsMap) {

		return process(element, annotationType, true, new Processor<AnnotationAttributes>() {
			@Override
			public AnnotationAttributes process(Annotation annotation, int metaDepth) {
				return AnnotationUtils.getAnnotationAttributes(annotation, classValuesAsString, nestedAnnotationsAsMap);
			}
			@Override
			public void postProcess(Annotation annotation, AnnotationAttributes result) {
				for (String key : result.keySet()) {
					if (!VALUE.equals(key)) {
						Object value = getValue(annotation, key);
						if (value != null) {
							result.put(key, value);
						}
					}
				}
			}
		});
	}

	public static MultiValueMap<String, Object> getAllAnnotationAttributes(AnnotatedElement element, String annotationType) {
		return getAllAnnotationAttributes(element, annotationType, false, false);
	}

	public static MultiValueMap<String, Object> getAllAnnotationAttributes(AnnotatedElement element,
			final String annotationType, final boolean classValuesAsString, final boolean nestedAnnotationsAsMap) {

		final MultiValueMap<String, Object> attributes = new LinkedMultiValueMap<String, Object>();
		process(element, annotationType, false, new Processor<Void>() {
			@Override
			public Void process(Annotation annotation, int metaDepth) {
				if (annotation.annotationType().getName().equals(annotationType)) {
					for (Map.Entry<String, Object> entry : AnnotationUtils.getAnnotationAttributes(annotation,
						classValuesAsString, nestedAnnotationsAsMap).entrySet()) {
						attributes.add(entry.getKey(), entry.getValue());
					}
				}
				return null;
			}
			@Override
			public void postProcess(Annotation annotation, Void result) {
				for (String key : attributes.keySet()) {
					if (!VALUE.equals(key)) {
						Object value = getValue(annotation, key);
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
	 * Process all annotations of the specified {@code annotationType} and
	 * recursively all meta-annotations on the specified {@code element}.
	 * <p>If the {@code traverseClassHierarchy} flag is {@code true} and the sought
	 * annotation is neither <em>directly present</em> on the given element nor
	 * present on the given element as a meta-annotation, then the algorithm will
	 * recursively search through the class hierarchy of the given element.
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @param traverseClassHierarchy whether or not to traverse up the class
	 * hierarchy recursively
	 * @param processor the processor to delegate to
	 * @return the result of the processor
	 */
	private static <T> T process(AnnotatedElement element, String annotationType, boolean traverseClassHierarchy,
			Processor<T> processor) {

		try {
			return doProcess(element, annotationType, traverseClassHierarchy, processor, new HashSet<AnnotatedElement>(), 0);
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Failed to introspect annotations: " + element, ex);
		}
	}

	/**
	 * Perform the search algorithm for the {@link #process} method, avoiding
	 * endless recursion by tracking which annotated elements have already been
	 * <em>visited</em>.
	 * <p>The {@code metaDepth} parameter represents the depth of the annotation
	 * relative to the initial element. For example, an annotation that is
	 * <em>present</em> on the element will have a depth of 0; a meta-annotation
	 * will have a depth of 1; and a meta-meta-annotation will have a depth of 2.
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @param traverseClassHierarchy whether or not to traverse up the class
	 * hierarchy recursively
	 * @param processor the processor to delegate to
	 * @param visited the set of annotated elements that have already been visited
	 * @param metaDepth the depth of the annotation relative to the initial element
	 * @return the result of the processor
	 */
	private static <T> T doProcess(AnnotatedElement element, String annotationType, boolean traverseClassHierarchy,
			Processor<T> processor, Set<AnnotatedElement> visited, int metaDepth) {

		if (visited.add(element)) {
			Annotation[] annotations =
					(traverseClassHierarchy ? element.getDeclaredAnnotations() : element.getAnnotations());
			for (Annotation annotation : annotations) {
				if (annotation.annotationType().getName().equals(annotationType) || metaDepth > 0) {
					T result = processor.process(annotation, metaDepth);
					if (result != null) {
						return result;
					}
					result = doProcess(annotation.annotationType(), annotationType, traverseClassHierarchy, processor,
						visited, metaDepth + 1);
					if (result != null) {
						processor.postProcess(annotation, result);
						return result;
					}
				}
			}
			for (Annotation annotation : annotations) {
				if (!isInJavaLangAnnotationPackage(annotation)) {
					T result = doProcess(annotation.annotationType(), annotationType, traverseClassHierarchy,
						processor, visited, metaDepth);
					if (result != null) {
						processor.postProcess(annotation, result);
						return result;
					}
				}
			}
			if (traverseClassHierarchy && element instanceof Class) {
				Class<?> superclass = ((Class<?>) element).getSuperclass();
				if (superclass != null && !superclass.equals(Object.class)) {
					T result = doProcess(superclass, annotationType, traverseClassHierarchy, processor, visited,
						metaDepth);
					if (result != null) {
						return result;
					}
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
		 * <p>The {@code metaDepth} parameter represents the depth of the
		 * annotation relative to the initial element. For example, an annotation
		 * that is <em>present</em> on the element will have a depth of 0; a
		 * meta-annotation will have a depth of 1; and a meta-meta-annotation
		 * will have a depth of 2.
		 * @param annotation the annotation to process
		 * @param metaDepth the depth of the annotation relative to the initial element
		 * @return the result of the processing or {@code null} to continue
		 */
		T process(Annotation annotation, int metaDepth);

		void postProcess(Annotation annotation, T result);
	}

}
