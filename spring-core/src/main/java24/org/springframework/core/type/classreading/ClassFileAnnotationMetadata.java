/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.type.classreading;

import java.lang.classfile.AccessFlags;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.attribute.InnerClassInfo;
import java.lang.classfile.attribute.InnerClassesAttribute;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import java.lang.reflect.AccessFlag;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link AnnotationMetadata} implementation that leverages
 * the {@link java.lang.classfile.ClassFile} API.
 *
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 7.0
 */
final class ClassFileAnnotationMetadata implements AnnotationMetadata {

	private final String className;

	private final AccessFlags accessFlags;

	private final @Nullable String enclosingClassName;

	private final @Nullable String superClassName;

	private final boolean independentInnerClass;

	private final Set<String> interfaceNames;

	private final Set<String> memberClassNames;

	private final Set<MethodMetadata> declaredMethods;

	private final MergedAnnotations mergedAnnotations;

	private @Nullable Set<String> annotationTypes;


	ClassFileAnnotationMetadata(String className, AccessFlags accessFlags, @Nullable String enclosingClassName,
				@Nullable String superClassName, boolean independentInnerClass, Set<String> interfaceNames,
				Set<String> memberClassNames, Set<MethodMetadata> declaredMethods, MergedAnnotations mergedAnnotations) {

		this.className = className;
		this.accessFlags = accessFlags;
		this.enclosingClassName = enclosingClassName;
		this.superClassName = (!className.endsWith(ClassUtils.PACKAGE_INFO_SUFFIX) ? superClassName : null);
		this.independentInnerClass = independentInnerClass;
		this.interfaceNames = interfaceNames;
		this.memberClassNames = memberClassNames;
		this.declaredMethods = declaredMethods;
		this.mergedAnnotations = mergedAnnotations;
	}


	@Override
	public String getClassName() {
		return this.className;
	}

	@Override
	public boolean isInterface() {
		return this.accessFlags.has(AccessFlag.INTERFACE);
	}

	@Override
	public boolean isAnnotation() {
		return this.accessFlags.has(AccessFlag.ANNOTATION);
	}

	@Override
	public boolean isAbstract() {
		return this.accessFlags.has(AccessFlag.ABSTRACT);
	}

	@Override
	public boolean isFinal() {
		return this.accessFlags.has(AccessFlag.FINAL);
	}

	@Override
	public boolean isIndependent() {
		return (this.enclosingClassName == null || this.independentInnerClass);
	}

	@Override
	public @Nullable String getEnclosingClassName() {
		return this.enclosingClassName;
	}

	@Override
	public @Nullable String getSuperClassName() {
		return this.superClassName;
	}

	@Override
	public String[] getInterfaceNames() {
		return StringUtils.toStringArray(this.interfaceNames);
	}

	@Override
	public String[] getMemberClassNames() {
		return StringUtils.toStringArray(this.memberClassNames);
	}

	@Override
	public MergedAnnotations getAnnotations() {
		return this.mergedAnnotations;
	}

	@Override
	public Set<String> getAnnotationTypes() {
		Set<String> annotationTypes = this.annotationTypes;
		if (annotationTypes == null) {
			annotationTypes = Collections.unmodifiableSet(AnnotationMetadata.super.getAnnotationTypes());
			this.annotationTypes = annotationTypes;
		}
		return annotationTypes;
	}

	@Override
	public Set<MethodMetadata> getAnnotatedMethods(String annotationName) {
		Set<MethodMetadata> result = new LinkedHashSet<>(4);
		for (MethodMetadata annotatedMethod : this.declaredMethods) {
			if (annotatedMethod.isAnnotated(annotationName)) {
				result.add(annotatedMethod);
			}
		}
		return Collections.unmodifiableSet(result);
	}

	@Override
	public Set<MethodMetadata> getDeclaredMethods() {
		return Collections.unmodifiableSet(this.declaredMethods);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof ClassFileAnnotationMetadata that &&
				this.className.equals(that.className)));
	}

	@Override
	public int hashCode() {
		return this.className.hashCode();
	}

	@Override
	public String toString() {
		return this.className;
	}


	static ClassFileAnnotationMetadata of(ClassModel classModel, @Nullable ClassLoader classLoader) {
		ClassEntry thisClass = classModel.thisClass();
		String currentClassName = thisClass.name().stringValue();
		String className = ClassUtils.convertResourcePathToClassName(currentClassName);
		AccessFlags accessFlags = classModel.flags();
		ClassEntry superClassEntry = classModel.superclass().orElse(null);
		String superClassName = (superClassEntry != null ?
				ClassUtils.convertResourcePathToClassName(superClassEntry.name().stringValue()) : null);

		List<ClassEntry> interfaces = classModel.interfaces();
		Set<String> interfaceNames;
		if (interfaces.isEmpty()) {
			interfaceNames = Collections.emptySet();
		}
		else {
			interfaceNames = CollectionUtils.newLinkedHashSet(interfaces.size());
			for (ClassEntry entry : interfaces) {
				interfaceNames.add(ClassUtils.convertResourcePathToClassName(entry.name().stringValue()));
			}
		}

		String enclosingClassName = null;
		boolean staticInnerClass = false;
		Set<String> memberClassNames = Collections.emptySet();
		InnerClassesAttribute innerClasses = classModel.findAttribute(Attributes.innerClasses()).orElse(null);
		if (innerClasses != null) {
			for (InnerClassInfo classInfo : innerClasses.classes()) {
				String innerClassName = classInfo.innerClass().name().stringValue();
				ClassEntry outerClass = classInfo.outerClass().orElse(null);
				if (currentClassName.equals(innerClassName)) {
					// the current class is an inner class
					staticInnerClass = (classInfo.flagsMask() & AccessFlag.STATIC.mask()) != 0;
					if (outerClass != null) {
						enclosingClassName = ClassUtils.convertResourcePathToClassName(outerClass.name().stringValue());
					}
				}
				if (outerClass != null && outerClass.name().stringValue().equals(currentClassName)) {
					// collecting data about actual inner classes
					if (memberClassNames.isEmpty()) {
						memberClassNames = new LinkedHashSet<>(4);
					}
					memberClassNames.add(ClassUtils.convertResourcePathToClassName(innerClassName));
				}
			}
		}
		if (enclosingClassName == null && classModel.findAttribute(Attributes.nestHost()).isPresent()) {
			int currentClassIndex = currentClassName.lastIndexOf('$');
			if (currentClassIndex > 0) {
				enclosingClassName = ClassUtils.convertResourcePathToClassName(
						currentClassName.substring(0, currentClassIndex));
			}
		}
		boolean independentInnerClass = (enclosingClassName != null && staticInnerClass);

		RuntimeVisibleAnnotationsAttribute annotationsAttribute =
				classModel.findAttribute(Attributes.runtimeVisibleAnnotations()).orElse(null);
		MergedAnnotations mergedAnnotations = (annotationsAttribute != null ?
				ClassFileAnnotationDelegate.createMergedAnnotations(className, annotationsAttribute, classLoader) :
				MergedAnnotations.of(Collections.emptyList()));

		List<MethodModel> methods = classModel.methods();
		Set<MethodMetadata> declaredMethods = Collections.emptySet();
		for (MethodModel method : methods) {
			if (!method.flags().has(AccessFlag.SYNTHETIC) && !method.methodName().equalsString("<init>")) {
				if (declaredMethods.isEmpty()) {
					declaredMethods = CollectionUtils.newLinkedHashSet(methods.size());
				}
				declaredMethods.add(ClassFileMethodMetadata.of(method, className, classLoader));
			}
		}

		return new ClassFileAnnotationMetadata(className, accessFlags, enclosingClassName, superClassName,
				independentInnerClass, interfaceNames, memberClassNames, declaredMethods, mergedAnnotations);
	}

	static String resolveTypeName(ClassDesc type) {
		if (type.isPrimitive()) {
			return type.displayName();
		}
		ClassDesc effectiveType = type;
		while (effectiveType.isArray()) {
			effectiveType = effectiveType.componentType();
		}
		String packageName = effectiveType.packageName();
		return (packageName.isEmpty() ? type.displayName() : packageName + "." + type.displayName());
	}

}
