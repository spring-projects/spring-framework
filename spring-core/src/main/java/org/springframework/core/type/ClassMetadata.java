/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.core.type;

import java.util.Set;

import org.springframework.lang.Nullable;

/**
 * Interface that defines abstract metadata of a specific class,
 * in a form that does not require that class to be loaded yet.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see StandardClassMetadata
 * @see org.springframework.core.type.classreading.MetadataReader#getClassMetadata()
 * @see AnnotationMetadata
 */
public interface ClassMetadata {

	/**
	 * Return the name of the underlying class.
	 */
	String getClassName();

	/**
	 * Return whether the underlying class represents an interface.
	 */
	boolean isInterface();

	/**
	 * Return whether the underlying class represents an annotation.
	 * @since 4.1
	 */
	boolean isAnnotation();

	/**
	 * Return whether the underlying class is marked as abstract.
	 */
	boolean isAbstract();

	/**
	 * Return whether the underlying class represents a concrete class,
	 * i.e. neither an interface nor an abstract class.
	 */
	default boolean isConcrete() {
		return !(isInterface() || isAbstract());
	}

	/**
	 * Return whether the underlying class is marked as 'final'.
	 */
	boolean isFinal();

	/**
	 * Determine whether the underlying class is independent, i.e. whether
	 * it is a top-level class or a nested class (static inner class) that
	 * can be constructed independently of an enclosing class.
	 */
	boolean isIndependent();

	/**
	 * Return whether the underlying class is declared within an enclosing
	 * class (i.e. the underlying class is an inner/nested class or a
	 * local class within a method).
	 * <p>If this method returns {@code false}, then the underlying
	 * class is a top-level class.
	 */
	default boolean hasEnclosingClass() {
		return (getEnclosingClassName() != null);
	}

	/**
	 * Return the name of the enclosing class of the underlying class,
	 * or {@code null} if the underlying class is a top-level class.
	 */
	@Nullable
	String getEnclosingClassName();

	/**
	 * Return whether the underlying class has a superclass.
	 *
	 * Note this differs from {@link Class#getSuperclass()}, and considers {@link Object} a super class.
	 */
	default boolean hasSuperClass() {
		return (getSuperClassName() != null);
	}

	/**
	 * Return the {@link ClassMetadata} for the enclosing class.
	 * @throws ClassMetadataNotFoundException if class metadata for the super class could
	 * not be loaded.
	 * @returns the {@link ClassMetadata} for the enclosing class. Null if the
	 * class isn't enclosed
	 * @since 6.x
	 */
	@Nullable
	ClassMetadata getEnclosingClassMetadata();

	/**
	 * Return the name of the superclass of the underlying class,
	 * or {@code null} if there is no superclass defined.
	 */
	@Nullable
	String getSuperClassName();

	/**
	 * Return the {@link ClassMetadata} for the super class.
	 * @throws ClassMetadataNotFoundException if class metadata for the super class could
	 * not be loaded.
	 * @since 6.x
	 */
	@Nullable
	ClassMetadata getSuperClassMetadata();

	/**
	 * Return the names of all interfaces that the underlying class
	 * implements, or an empty array if there are none.
	 */
	String[] getInterfaceNames();

	/**
	 * Return the {@link ClassMetadata} of the interfaces this class directly
	 * implements.
	 * @return a set of {@link ClassMetadata} containing the interface classes
	 * for the class
	 * @throws ClassMetadataNotFoundException if class metadata for the interface classes
	 * coudl not be loaded.
	 * @since 6.x
	 */
	Set<ClassMetadata> getInterfaceClassMetadata();

	/**
	 * Return the names of all classes declared as members of the class represented by
	 * this ClassMetadata object. This includes public, protected, default (package)
	 * access, and private classes and interfaces declared by the class, but excludes
	 * inherited classes and interfaces. An empty array is returned if no member classes
	 * or interfaces exist.
	 * @since 3.1
	 */
	String[] getMemberClassNames();

	/**
	 * Return the {@link ClassMetadata} of the member clases of this class.
	 * @return a set of {@link ClassMetadata} containing the interface classes
	 * for the class
	 * @throws ClassMetadataNotFoundException if class metadata for the interface classes
	 * coudl not be loaded.
	 * @since 6.x
	 */
	Set<ClassMetadata> getMemberClassMetadata();

	/**
	 * Satisfies {@link Class#isEnum()}.
	 * @since 6.x
	 */
	boolean isEnum();

	/**
	 * Satisfies {@link Class#isPrimitive()}.
	 * @since 6.x
	 */
	boolean isPrimitive();

	/**
	 * Satisfies {@link Class#isSynthetic()}.
	 * @since 6.x
	 */
	boolean isSynthetic();

	/**
	 * Return the underlying modifiers for the class.
	 * @since 6.x
	 */
	int getModifiers();

	/**
	 * Retrieve the method metadata for all user-declared methods on the underlying class,
	 * preserving declaration order as far as possible.
	 * @return a set of {@link MethodMetadata}
	 * @since 6.x
	 */
	Set<MethodMetadata> getDeclaredMethods();

	/**
	 * Retrieve the method metadata for a given declared field.
	 * @since 6.x
	 */
	FieldMetadata getDeclaredField(String name);

	/**
	 * Retrieve the method metadata for all user-declared fields on the underlying class,
	 * preserving declaration order as far as possible.
	 * @return a set of {@link FieldMetadata}
	 * @since 6.x
	 */
	Set<FieldMetadata> getDeclaredFields();

	/**
	 * Retrieve the method metadata for all user-declared constructors.
	 * @return a set of {@link ConstructorMetadata}
	 * @since 6.x
	 */
	Set<ConstructorMetadata> getDeclaredConstructors();

	/**
	 * Determine if the given class is exactly this type.
	 * @throws NullPointerException if clazz is null
	 * @throws ClassMetadataNotFoundException if class metadata for the class hierarchy of
	 * this class could not be loaded
	 * @since 6.x
	 */
	boolean isType(Class<?> clazz);

	/**
	 * Determine if the given class name is exactly this type.
	 * @throws NullPointerException if className is null
	 * @throws ClassMetadataNotFoundException if class metadata for the class hierarchy of
	 * this class could not be loaded
	 * @since 6.x
	 */
	boolean isType(String className);

	/**
	 * Determine if the given type is, implements or extends the given class.
	 * @throws NullPointerException if clazz is null
	 * @throws ClassMetadataNotFoundException if class metadata for the class hierarchy of
	 * this class could not be loaded
	 * @since 6.x
	 */
	boolean isAssignableTo(Class<?> clazz);

	/**
	 * Determine if the given type is, implements or extends the given class.
	 * @throws NullPointerException if className is null
	 * @throws ClassMetadataNotFoundException if class metadata for the class hierarchy of
	 * this class could not be loaded
	 * @since 6.x
	 */
	boolean isAssignableTo(String className);

}
