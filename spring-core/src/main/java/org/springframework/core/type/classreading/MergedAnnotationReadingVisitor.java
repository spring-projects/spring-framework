/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.type.classreading;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.asm.Type;
import org.springframework.core.annotation.AnnotationFilter;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.util.ClassUtils;

/**
 * {@link AnnotationVisitor} that can be used to construct a
 * {@link MergedAnnotation}.
 *
 * @author Phillip Webb
 * @since 5.2
 * @param <A> the annotation type
 */
class MergedAnnotationReadingVisitor<A extends Annotation> extends AnnotationVisitor {

	private final @Nullable ClassLoader classLoader;

	private final @Nullable Object source;

	private final Class<A> annotationType;

	private final Consumer<MergedAnnotation<A>> consumer;

	private final Map<String, Object> attributes = new LinkedHashMap<>(4);


	public MergedAnnotationReadingVisitor(@Nullable ClassLoader classLoader, @Nullable Object source,
			Class<A> annotationType, Consumer<MergedAnnotation<A>> consumer) {

		super(SpringAsmInfo.ASM_VERSION);
		this.classLoader = classLoader;
		this.source = source;
		this.annotationType = annotationType;
		this.consumer = consumer;
	}


	@Override
	public void visit(String name, Object value) {
		if (value instanceof Type type) {
			value = type.getClassName();
		}
		this.attributes.put(name, value);
	}

	@Override
	public void visitEnum(String name, String descriptor, String value) {
		visitEnum(descriptor, value, enumValue -> this.attributes.put(name, enumValue));
	}

	@Override
	public @Nullable AnnotationVisitor visitAnnotation(String name, String descriptor) {
		return visitAnnotation(descriptor, annotation -> this.attributes.put(name, annotation));
	}

	@Override
	public AnnotationVisitor visitArray(String name) {
		return new ArrayVisitor(value -> this.attributes.put(name, value));
	}

	@Override
	public void visitEnd() {
		Map<String, Object> compactedAttributes =
				(this.attributes.isEmpty() ? Collections.emptyMap() : this.attributes);
		MergedAnnotation<A> annotation = MergedAnnotation.of(
				this.classLoader, this.source, this.annotationType, compactedAttributes);
		this.consumer.accept(annotation);
	}

	@SuppressWarnings("unchecked")
	public <E extends Enum<E>> void visitEnum(String descriptor, String value, Consumer<E> consumer) {
		String className = Type.getType(descriptor).getClassName();
		Class<E> type = null;
		try {
			type = (Class<E>) ClassUtils.forName(className, this.classLoader);
		}
		catch (ClassNotFoundException | LinkageError ex) {
			throw new TypeNotPresentException(className, ex);
		}
		consumer.accept(Enum.valueOf(type, value));
	}

	@SuppressWarnings("unchecked")
	private <T extends Annotation> @Nullable AnnotationVisitor visitAnnotation(
			String descriptor, Consumer<MergedAnnotation<T>> consumer) {

		String className = Type.getType(descriptor).getClassName();
		if (AnnotationFilter.PLAIN.matches(className)) {
			return null;
		}
		Class<T> type = null;
		try {
			type = (Class<T>) ClassUtils.forName(className, this.classLoader);
		}
		catch (ClassNotFoundException | LinkageError ex) {
			throw new TypeNotPresentException(className, ex);
		}
		return new MergedAnnotationReadingVisitor<>(this.classLoader, this.source, type, consumer);
	}


	@SuppressWarnings("unchecked")
	static <A extends Annotation> @Nullable AnnotationVisitor get(@Nullable ClassLoader classLoader,
			@Nullable Object source, String descriptor, boolean visible,
			Consumer<MergedAnnotation<A>> consumer) {

		if (!visible) {
			return null;
		}

		String typeName = Type.getType(descriptor).getClassName();
		if (AnnotationFilter.PLAIN.matches(typeName)) {
			return null;
		}

		try {
			Class<A> annotationType = (Class<A>) ClassUtils.forName(typeName, classLoader);
			return new MergedAnnotationReadingVisitor<>(classLoader, source, annotationType, consumer);
		}
		catch (ClassNotFoundException | LinkageError ex) {
			return null;
		}
	}


	/**
	 * {@link AnnotationVisitor} to deal with array attributes.
	 */
	private class ArrayVisitor extends AnnotationVisitor {

		private final List<Object> elements = new ArrayList<>();

		private final Consumer<Object[]> consumer;

		ArrayVisitor(Consumer<Object[]> consumer) {
			super(SpringAsmInfo.ASM_VERSION);
			this.consumer = consumer;
		}

		@Override
		public void visit(String name, Object value) {
			if (value instanceof Type type) {
				value = type.getClassName();
			}
			this.elements.add(value);
		}

		@Override
		public void visitEnum(String name, String descriptor, String value) {
			MergedAnnotationReadingVisitor.this.visitEnum(descriptor, value, this.elements::add);
		}

		@Override
		public @Nullable AnnotationVisitor visitAnnotation(String name, String descriptor) {
			return MergedAnnotationReadingVisitor.this.visitAnnotation(descriptor, this.elements::add);
		}

		@Override
		public void visitEnd() {
			Class<?> componentType = getComponentType();
			Object[] array = (Object[]) Array.newInstance(componentType, this.elements.size());
			this.consumer.accept(this.elements.toArray(array));
		}

		private Class<?> getComponentType() {
			if (this.elements.isEmpty()) {
				return Object.class;
			}
			Object firstElement = this.elements.get(0);
			if (firstElement instanceof Enum<?> enumeration) {
				return enumeration.getDeclaringClass();
			}
			return firstElement.getClass();
		}
	}

}
