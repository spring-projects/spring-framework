/*
 * Copyright 2002-2011 the original author or authors.
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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Type;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * ASM class visitor which looks for the class name and implemented types as
 * well as for the annotations defined on the class, exposing them through
 * the {@link org.springframework.core.type.AnnotationMetadata} interface.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Costin Leau
 * @since 2.5
 */
final class AnnotationMetadataReadingVisitor extends ClassMetadataReadingVisitor implements AnnotationMetadata {

	private final ClassLoader classLoader;

	private final Set<String> annotationSet = new LinkedHashSet<String>();

	private final Map<String, Set<String>> metaAnnotationMap = new LinkedHashMap<String, Set<String>>(4);

	private final Map<String, Map<String, Object>> attributeMap = new LinkedHashMap<String, Map<String, Object>>(4);

	private final MultiValueMap<String, MethodMetadata> methodMetadataMap = new LinkedMultiValueMap<String, MethodMetadata>();


	public AnnotationMetadataReadingVisitor(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		return new MethodMetadataReadingVisitor(name, getReturnTypeFromAsmMethodDescriptor(desc), access, this.getClassName(), this.classLoader, this.methodMetadataMap);
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
		String className = Type.getType(desc).getClassName();
		this.annotationSet.add(className);
		return new AnnotationAttributesReadingVisitor(className, this.attributeMap, this.metaAnnotationMap, this.classLoader);
	}


	public Set<String> getAnnotationTypes() {
		return this.annotationSet;
	}

	public Set<String> getMetaAnnotationTypes(String annotationType) {
		return this.metaAnnotationMap.get(annotationType);
	}

	public boolean hasAnnotation(String annotationType) {
		return this.annotationSet.contains(annotationType);
	}

	public boolean hasMetaAnnotation(String metaAnnotationType) {
		Collection<Set<String>> allMetaTypes = this.metaAnnotationMap.values();
		for (Set<String> metaTypes : allMetaTypes) {
			if (metaTypes.contains(metaAnnotationType)) {
				return true;
			}
		}
		return false;
	}

	public boolean isAnnotated(String annotationType) {
		return this.attributeMap.containsKey(annotationType);
	}

	public Map<String, Object> getAnnotationAttributes(String annotationType) {
		return getAnnotationAttributes(annotationType, false);
	}

	public Map<String, Object> getAnnotationAttributes(String annotationType, boolean classValuesAsString) {
		Map<String, Object> raw = this.attributeMap.get(annotationType);
		if (raw == null) {
			return null;
		}
		Map<String, Object> result = new LinkedHashMap<String, Object>(raw.size());
		for (Map.Entry<String, Object> entry : raw.entrySet()) {
			try {
				Object value = entry.getValue();
				if (value instanceof Type) {
					value = (classValuesAsString ? ((Type) value).getClassName() :
							this.classLoader.loadClass(((Type) value).getClassName()));
				}
				else if (value instanceof Type[]) {
					Type[] array = (Type[]) value;
					Object[] convArray = (classValuesAsString ? new String[array.length] : new Class[array.length]);
					for (int i = 0; i < array.length; i++) {
						convArray[i] = (classValuesAsString ? array[i].getClassName() :
								this.classLoader.loadClass(array[i].getClassName()));
					}
					value = convArray;
				}
				else if (classValuesAsString) {
					if (value instanceof Class) {
						value = ((Class) value).getName();
					}
					else if (value instanceof Class[]) {
						Class[] clazzArray = (Class[]) value;
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
				// Class not found - can't resolve class reference in annotation attribute.
			}
		}
		return result;
	}

	public boolean hasAnnotatedMethods(String annotationType) {
		return this.methodMetadataMap.containsKey(annotationType);
	}

	public Set<MethodMetadata> getAnnotatedMethods(String annotationType) {
		List<MethodMetadata> list = this.methodMetadataMap.get(annotationType);
		if (CollectionUtils.isEmpty(list)) {
			return new LinkedHashSet<MethodMetadata>(0);
		}
		Set<MethodMetadata> annotatedMethods = new LinkedHashSet<MethodMetadata>(list.size());
		annotatedMethods.addAll(list);
		return annotatedMethods;
	}

	/**
	 * Convert a type descriptor to a classname suitable for classloading with
	 * Class.forName().
	 * 
	 * @param typeDescriptor see ASM guide section 2.1.3
	 */
	private static String convertAsmTypeDescriptorToClassName(String typeDescriptor) {
		final String internalName; // See ASM guide section 2.1.2

		if ("V".equals(typeDescriptor))
			return Void.class.getName();
		if ("I".equals(typeDescriptor))
			return Integer.class.getName();
		if ("Z".equals(typeDescriptor))
			return Boolean.class.getName();

		// strip the leading array/object/primitive identifier
		if (typeDescriptor.startsWith("[["))
			internalName = typeDescriptor.substring(3);
		else if (typeDescriptor.startsWith("["))
			internalName = typeDescriptor.substring(2);
		else
			internalName = typeDescriptor.substring(1);

		// convert slashes to dots
		String className = internalName.replace('/', '.');

		// and strip trailing semicolon (if present)
		if (className.endsWith(";"))
			className = className.substring(0, internalName.length() - 1);

		return className;
	}

	/**
	 * @param methodDescriptor see ASM guide section 2.1.4
	 */
	private static String getReturnTypeFromAsmMethodDescriptor(String methodDescriptor) {
		String returnTypeDescriptor = methodDescriptor.substring(methodDescriptor.indexOf(')') + 1);
		return convertAsmTypeDescriptorToClassName(returnTypeDescriptor);
	}

}
