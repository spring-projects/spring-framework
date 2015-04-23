/*
 * Copyright 2002-2015 the original author or authors.
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
 * Utility class used to collect all annotation attributes, including those
 * declared on meta-annotations.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
public class AnnotatedElementUtils {

	public static Set<String> getMetaAnnotationTypes(AnnotatedElement element, String annotationType) {
		final Set<String> types = new LinkedHashSet<String>();
		process(element, annotationType, true, false, new Processor<Object>() {
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
		return Boolean.TRUE.equals(process(element, annotationType, true, false, new Processor<Boolean>() {
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
		return Boolean.TRUE.equals(process(element, annotationType, true, false, new Processor<Boolean>() {
			@Override
			public Boolean process(Annotation annotation, int metaDepth) {
				return Boolean.TRUE;
			}
			@Override
			public void postProcess(Annotation annotation, Boolean result) {
			}
		}));
	}

	/**
	 * Delegates to {@link #getAnnotationAttributes(AnnotatedElement, String, boolean, boolean)},
	 * supplying {@code false} for {@code classValuesAsString} and {@code nestedAnnotationsAsMap}.
	 *
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @see #getAnnotationAttributes(AnnotatedElement, String, boolean, boolean)
	 */
	public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement element, String annotationType) {
		return getAnnotationAttributes(element, annotationType, false, false);
	}

	/**
	 * Delegates to {@link #getAnnotationAttributes(AnnotatedElement, String, boolean, boolean, boolean, boolean)},
	 * supplying {@code true} for {@code searchInterfaces} and {@code false} for {@code searchClassHierarchy}.
	 *
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @param classValuesAsString whether to convert Class references into
	 * Strings or to preserve them as Class references
	 * @param nestedAnnotationsAsMap whether to turn nested Annotation instances
	 * into {@link AnnotationAttributes} maps or to preserve them as Annotation
	 * instances
	 * @see #getAnnotationAttributes(AnnotatedElement, String, boolean, boolean, boolean, boolean)
	 */
	public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement element, String annotationType,
			boolean classValuesAsString, boolean nestedAnnotationsAsMap) {
		return getAnnotationAttributes(element, annotationType, true, false, classValuesAsString,
			nestedAnnotationsAsMap);
	}

	/**
	 * Find annotation attributes of the specified {@code annotationType} in
	 * the annotation hierarchy of the supplied {@link AnnotatedElement},
	 * and merge the results into an {@link AnnotationAttributes} map.
	 *
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @param searchInterfaces whether or not to search on interfaces, if the
	 * annotated element is a class
	 * @param searchClassHierarchy whether or not to search the class hierarchy
	 * recursively, if the annotated element is a class
	 * @param classValuesAsString whether to convert Class references into
	 * Strings or to preserve them as Class references
	 * @param nestedAnnotationsAsMap whether to turn nested Annotation instances
	 * into {@link AnnotationAttributes} maps or to preserve them as Annotation
	 * instances
	 */
	public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement element, String annotationType,
			boolean searchInterfaces, boolean searchClassHierarchy, final boolean classValuesAsString,
			final boolean nestedAnnotationsAsMap) {

		return process(element, annotationType, searchInterfaces, searchClassHierarchy, new Processor<AnnotationAttributes>() {
			@Override
			public AnnotationAttributes process(Annotation annotation, int metaDepth) {
				return AnnotationUtils.getAnnotationAttributes(annotation, classValuesAsString, nestedAnnotationsAsMap);
			}
			@Override
			public void postProcess(Annotation annotation, AnnotationAttributes result) {
				for (String key : result.keySet()) {
					if (!AnnotationUtils.VALUE.equals(key)) {
						Object value = AnnotationUtils.getValue(annotation, key);
						if (value != null) {
							result.put(key, AnnotationUtils.adaptValue(value, classValuesAsString, nestedAnnotationsAsMap));
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
		process(element, annotationType, true, false, new Processor<Void>() {
			@Override
			public Void process(Annotation annotation, int metaDepth) {
				if (annotation.annotationType().getName().equals(annotationType)) {
					for (Map.Entry<String, Object> entry : AnnotationUtils.getAnnotationAttributes(
							annotation, classValuesAsString, nestedAnnotationsAsMap).entrySet()) {
						attributes.add(entry.getKey(), entry.getValue());
					}
				}
				return null;
			}
			@Override
			public void postProcess(Annotation annotation, Void result) {
				for (String key : attributes.keySet()) {
					if (!AnnotationUtils.VALUE.equals(key)) {
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
	 * Process all annotations of the specified {@code annotationType} and
	 * recursively all meta-annotations on the specified {@code element}.
	 *
	 * <p>If the {@code searchClassHierarchy} flag is {@code true} and the sought
	 * annotation is neither <em>directly present</em> on the given element nor
	 * present on the given element as a meta-annotation, then the algorithm will
	 * recursively search through the class hierarchy of the given element.
	 *
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @param searchInterfaces whether or not to search on interfaces, if the
	 * annotated element is a class
	 * @param searchClassHierarchy whether or not to search the class hierarchy
	 * recursively, if the annotated element is a class
	 * @param processor the processor to delegate to
	 * @return the result of the processor
	 */
	private static <T> T process(AnnotatedElement element, String annotationType, boolean searchInterfaces,
			boolean searchClassHierarchy, Processor<T> processor) {

		try {
			return doProcess(element, annotationType, searchInterfaces, searchClassHierarchy, processor,
					new HashSet<AnnotatedElement>(), 0);
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Failed to introspect annotations: " + element, ex);
		}
	}

	/**
	 * Perform the search algorithm for the {@link #process} method, avoiding
	 * endless recursion by tracking which annotated elements have already been
	 * <em>visited</em>.
	 *
	 * <p>The {@code metaDepth} parameter represents the depth of the annotation
	 * relative to the initial element. For example, an annotation that is
	 * <em>present</em> on the element will have a depth of 0; a meta-annotation
	 * will have a depth of 1; and a meta-meta-annotation will have a depth of 2.
	 *
	 * @param element the annotated element
	 * @param annotationType the annotation type to find
	 * @param searchInterfaces whether or not to search on interfaces, if the
	 * annotated element is a class
	 * @param searchClassHierarchy whether or not to search the class hierarchy
	 * recursively, if the annotated element is a class
	 * @param processor the processor to delegate to
	 * @param visited the set of annotated elements that have already been visited
	 * @param metaDepth the depth of the annotation relative to the initial element
	 * @return the result of the processor
	 */
	private static <T> T doProcess(AnnotatedElement element, String annotationType, boolean searchInterfaces,
			boolean searchClassHierarchy, Processor<T> processor, Set<AnnotatedElement> visited, int metaDepth) {

		if (visited.add(element)) {
			try {

				// Local annotations: declared or (declared + inherited).
				Annotation[] annotations =
						(searchClassHierarchy ? element.getDeclaredAnnotations() : element.getAnnotations());

				// Search in local annotations
				for (Annotation annotation : annotations) {
					if (annotation.annotationType().getName().equals(annotationType) || metaDepth > 0) {
						T result = processor.process(annotation, metaDepth);
						if (result != null) {
							return result;
						}
						result = doProcess(annotation.annotationType(), annotationType, searchInterfaces,
							searchClassHierarchy, processor, visited, metaDepth + 1);
						if (result != null) {
							processor.postProcess(annotation, result);
							return result;
						}
					}
				}

				// Search in meta annotations on local annotations
				for (Annotation annotation : annotations) {
					if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotation)) {
						T result = doProcess(annotation.annotationType(), annotationType, searchInterfaces,
							searchClassHierarchy, processor, visited, metaDepth);
						if (result != null) {
							processor.postProcess(annotation, result);
							return result;
						}
					}
				}

				// Search on interfaces
				if (searchInterfaces && element instanceof Class) {
					Class<?> clazz = (Class<?>) element;
					for (Class<?> ifc : clazz.getInterfaces()) {
						T result = doProcess(ifc, annotationType, searchInterfaces, searchClassHierarchy, processor,
							visited, metaDepth);
						if (result != null) {
							return result;
						}
					}
				}

				// Search on superclass
				if (searchClassHierarchy && element instanceof Class) {
					Class<?> superclass = ((Class<?>) element).getSuperclass();
					if (superclass != null && !superclass.equals(Object.class)) {
						T result = doProcess(superclass, annotationType, searchInterfaces, searchClassHierarchy,
							processor, visited, metaDepth);
						if (result != null) {
							return result;
						}
					}
				}
			}
			catch (Exception ex) {
				AnnotationUtils.logIntrospectionFailure(element, ex);
			}
		}
		return null;
	}


	/**
	 * Callback interface used to process an annotation.
	 * @param <T> the result type
	 */
	private interface Processor<T> {

		/**
		 * Called to process the annotation.
		 * <p>The {@code metaDepth} parameter represents the depth of the
		 * annotation relative to the initial element. For example, an annotation
		 * that is <em>present</em> on the element will have a depth of 0; a
		 * meta-annotation will have a depth of 1; and a meta-meta-annotation
		 * will have a depth of 2.
		 * @param annotation the annotation to process
		 * @param metaDepth the depth of the annotation relative to the initial element
		 * @return the result of the processing, or {@code null} to continue
		 */
		T process(Annotation annotation, int metaDepth);

		void postProcess(Annotation annotation, T result);
	}

}
