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

package org.springframework.core.type.classreading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.asm.Type;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.LinkedMultiValueMap;

import static org.springframework.core.annotation.AnnotationUtils.*;

/**
 * Internal utility class used when reading annotations.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Costin Leau
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 */
abstract class AnnotationReadingVisitorUtils {

	public static AnnotationAttributes convertClassValues(ClassLoader classLoader, AnnotationAttributes original,
			boolean classValuesAsString) {

		if (original == null) {
			return null;
		}

		AnnotationAttributes result = new AnnotationAttributes(original.size());
		for (Map.Entry<String, Object> entry : original.entrySet()) {
			try {
				Object value = entry.getValue();
				if (value instanceof AnnotationAttributes) {
					value = convertClassValues(classLoader, (AnnotationAttributes) value, classValuesAsString);
				}
				else if (value instanceof AnnotationAttributes[]) {
					AnnotationAttributes[] values = (AnnotationAttributes[]) value;
					for (int i = 0; i < values.length; i++) {
						values[i] = convertClassValues(classLoader, values[i], classValuesAsString);
					}
				}
				else if (value instanceof Type) {
					value = (classValuesAsString ? ((Type) value).getClassName()
							: classLoader.loadClass(((Type) value).getClassName()));
				}
				else if (value instanceof Type[]) {
					Type[] array = (Type[]) value;
					Object[] convArray = (classValuesAsString ? new String[array.length] : new Class<?>[array.length]);
					for (int i = 0; i < array.length; i++) {
						convArray[i] = (classValuesAsString ? array[i].getClassName()
								: classLoader.loadClass(array[i].getClassName()));
					}
					value = convArray;
				}
				else if (classValuesAsString) {
					if (value instanceof Class) {
						value = ((Class<?>) value).getName();
					}
					else if (value instanceof Class[]) {
						Class<?>[] clazzArray = (Class[]) value;
						String[] newValue = new String[clazzArray.length];
						for (int i = 0; i < clazzArray.length; i++) {
							newValue[i] = clazzArray[i].getName();
						}
						value = newValue;
					}
				}
				result.put(entry.getKey(), value);
			}
			catch (Exception ex) {
				// Class not found - can't resolve class reference in annotation
				// attribute.
			}
		}
		return result;
	}

	/**
	 * Retrieve the merged attributes of the annotation of the given type, if any,
	 * from the supplied {@code attributeMap}.
	 * <p>Annotation attribute values appearing <em>lower</em> in the annotation
	 * hierarchy (i.e., closer to the declaring class) will override those
	 * defined <em>higher</em> in the annotation hierarchy.
	 * @param attributeMap the map of annotation attribute lists, keyed by
	 * annotation type
	 * @param annotationType the annotation type to look for
	 * @return the merged annotation attributes; or {@code null} if no matching
	 * annotation is present in the {@code attributeMap}
	 * @since 4.0.3
	 */
	public static AnnotationAttributes getMergedAnnotationAttributes(
			LinkedMultiValueMap<String, AnnotationAttributes> attributeMap, String annotationType) {

		// Get the unmerged list of attributes for the target annotation.
		List<AnnotationAttributes> attributesList = attributeMap.get(annotationType);
		if (attributesList == null || attributesList.isEmpty()) {
			return null;
		}

		// To start with, we populate the results with all attribute values from the
		// target annotation.
		AnnotationAttributes results = attributesList.get(0);
		Set<String> supportedAttributeNames = results.keySet();

		// Since the map is a LinkedMultiValueMap, we depend on the ordering of
		// elements in the map and reverse the order of the keys in order to traverse
		// "down" the meta-annotation hierarchy.
		List<String> annotationTypes = new ArrayList<String>(attributeMap.keySet());
		Collections.reverse(annotationTypes);

		for (String currentAnnotationType : annotationTypes) {
			if (!currentAnnotationType.startsWith("java.lang.annotation")) {
				for (String attributeName : supportedAttributeNames) {
					if (!VALUE.equals(attributeName)) {
						List<AnnotationAttributes> currentAttributes = attributeMap.get(currentAnnotationType);
						if (currentAttributes != null && !currentAttributes.isEmpty()) {
							Object value = currentAttributes.get(0).get(attributeName);
							if (value != null) {
								// Overwrite value from target annotation with the value
								// from an attribute of the same name found lower in the
								// meta-annotation hierarchy.
								results.put(attributeName, value);
							}
						}
					}
				}
			}
		}

		return results;
	}

}
