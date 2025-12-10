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

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.lang.Contract;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;

/**
 * Strategy used to find repeatable annotations within container annotations.
 *
 * <p>{@link #standardRepeatables() RepeatableContainers.standardRepeatables()}
 * provides a default strategy that respects Java's {@link Repeatable @Repeatable}
 * support and is suitable for most situations.
 *
 * <p>If you need to register repeatable annotation types that do not make use of
 * {@code @Repeatable}, you should typically use {@code standardRepeatables()}
 * combined with {@link #plus(Class, Class)}. Note that multiple invocations of
 * {@code plus()} can be chained together to register multiple repeatable/container
 * type pairs. For example:
 *
 * <pre class="code">
 * RepeatableContainers repeatableContainers =
 *     RepeatableContainers.standardRepeatables()
 *         .plus(MyRepeatable1.class, MyContainer1.class)
 *         .plus(MyRepeatable2.class, MyContainer2.class);</pre>
 *
 * <p>For special use cases where you are certain that you do not need Java's
 * {@code @Repeatable} support, you can use {@link #explicitRepeatable(Class, Class)
 * RepeatableContainers.explicitRepeatable()} to create an instance of
 * {@code RepeatableContainers} that only supports explicit repeatable/container
 * type pairs. As with {@code standardRepeatables()}, {@code plus()} can be used
 * to register additional repeatable/container type pairs. For example:
 *
 * <pre class="code">
 * RepeatableContainers repeatableContainers =
 *     RepeatableContainers.explicitRepeatable(MyRepeatable1.class, MyContainer1.class)
 *         .plus(MyRepeatable2.class, MyContainer2.class);</pre>
 *
 * <p>To completely disable repeatable annotation support use
 * {@link #none() RepeatableContainers.none()}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2
 */
public abstract class RepeatableContainers {

	static final Map<Class<? extends Annotation>, Object> cache = new ConcurrentReferenceHashMap<>();

	private final @Nullable RepeatableContainers parent;


	private RepeatableContainers(@Nullable RepeatableContainers parent) {
		this.parent = parent;
	}

	/**
	 * Register a pair of repeatable and container annotation types.
	 * <p>See the {@linkplain RepeatableContainers class-level javadoc} for examples.
	 * @param repeatable the repeatable annotation type
	 * @param container the container annotation type
	 * @return a new {@code RepeatableContainers} instance that is chained to
	 * the current instance
	 * @since 7.0
	 */
	public final RepeatableContainers plus(Class<? extends Annotation> repeatable,
			Class<? extends Annotation> container) {

		return new ExplicitRepeatableContainer(this, repeatable, container);
	}

	/**
	 * Register a pair of container and repeatable annotation types.
	 * <p><strong>WARNING</strong>: The arguments supplied to this method are in
	 * the reverse order of those supplied to {@link #plus(Class, Class)},
	 * {@link #explicitRepeatable(Class, Class)}, and {@link #of(Class, Class)}.
	 * @param container the container annotation type
	 * @param repeatable the repeatable annotation type
	 * @return a new {@code RepeatableContainers} instance that is chained to
	 * the current instance
	 * @deprecated as of Spring Framework 7.0, in favor of {@link #plus(Class, Class)}
	 */
	@Deprecated(since = "7.0")
	public RepeatableContainers and(Class<? extends Annotation> container,
			Class<? extends Annotation> repeatable) {

		return plus(repeatable, container);
	}

	/**
	 * Find repeated annotations contained in the supplied {@code annotation}.
	 * @param annotation the candidate container annotation
	 * @return the repeated annotations found in the supplied container annotation
	 * (potentially an empty array), or {@code null} if the supplied annotation is
	 * not a supported container annotation
	 */
	Annotation @Nullable [] findRepeatedAnnotations(Annotation annotation) {
		if (this.parent == null) {
			return null;
		}
		return this.parent.findRepeatedAnnotations(annotation);
	}


	@Override
	@Contract("null -> false")
	public boolean equals(@Nullable Object other) {
		if (other == this) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		return ObjectUtils.nullSafeEquals(this.parent, ((RepeatableContainers) other).parent);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.parent);
	}


	/**
	 * Create a {@link RepeatableContainers} instance that searches for repeated
	 * annotations according to the semantics of Java's {@link Repeatable @Repeatable}
	 * annotation.
	 * <p>See the {@linkplain RepeatableContainers class-level javadoc} for examples.
	 * @return a {@code RepeatableContainers} instance that supports {@code @Repeatable}
	 * @see #plus(Class, Class)
	 */
	public static RepeatableContainers standardRepeatables() {
		return StandardRepeatableContainers.INSTANCE;
	}

	/**
	 * Create a {@link RepeatableContainers} instance that searches for repeated
	 * annotations by taking into account the supplied repeatable and container
	 * annotation types.
	 * <p><strong>WARNING</strong>: The {@code RepeatableContainers} instance
	 * returned by this factory method does <strong>not</strong> respect Java's
	 * {@link Repeatable @Repeatable} support. Use {@link #standardRepeatables()}
	 * for standard {@code @Repeatable} support, optionally combined with
	 * {@link #plus(Class, Class)}.
	 * <p>If the supplied container annotation type is not {@code null}, it must
	 * declare a {@code value} attribute returning an array of repeatable
	 * annotations. If the supplied container annotation type is {@code null}, the
	 * container will be deduced by inspecting the {@code @Repeatable} annotation
	 * on the {@code repeatable} annotation type.
	 * <p>See the {@linkplain RepeatableContainers class-level javadoc} for examples.
	 * @param repeatable the repeatable annotation type
	 * @param container the container annotation type or {@code null}
	 * @return a {@code RepeatableContainers} instance that does not support
	 * {@link Repeatable @Repeatable}
	 * @throws IllegalArgumentException if the supplied container type is
	 * {@code null} and the annotation type is not a repeatable annotation
	 * @throws AnnotationConfigurationException if the supplied container type
	 * is not a properly configured container for a repeatable annotation
	 * @since 7.0
	 * @see #standardRepeatables()
	 * @see #plus(Class, Class)
	 */
	public static RepeatableContainers explicitRepeatable(
			Class<? extends Annotation> repeatable, @Nullable Class<? extends Annotation> container) {

		return new ExplicitRepeatableContainer(null, repeatable, container);
	}

	/**
	 * Create a {@link RepeatableContainers} instance that searches for repeated
	 * annotations by taking into account the supplied repeatable and container
	 * annotation types.
	 * <p><strong>WARNING</strong>: The {@code RepeatableContainers} instance
	 * returned by this factory method does <strong>not</strong> respect Java's
	 * {@link Repeatable @Repeatable} support. Use {@link #standardRepeatables()}
	 * for standard {@code @Repeatable} support, optionally combined with
	 * {@link #plus(Class, Class)}.
	 * <p><strong>WARNING</strong>: The arguments supplied to this method are in
	 * the reverse order of those supplied to {@link #and(Class, Class)}.
	 * <p>If the supplied container annotation type is not {@code null}, it must
	 * declare a {@code value} attribute returning an array of repeatable
	 * annotations. If the supplied container annotation type is {@code null}, the
	 * container will be deduced by inspecting the {@code @Repeatable} annotation
	 * on the {@code repeatable} annotation type.
	 * @param repeatable the repeatable annotation type
	 * @param container the container annotation type or {@code null}
	 * @return a {@code RepeatableContainers} instance that does not support
	 * {@link Repeatable @Repeatable}
	 * @throws IllegalArgumentException if the supplied container type is
	 * {@code null} and the annotation type is not a repeatable annotation
	 * @throws AnnotationConfigurationException if the supplied container type
	 * is not a properly configured container for a repeatable annotation
	 * @deprecated as of Spring Framework 7.0, in favor of {@link #explicitRepeatable(Class, Class)}
	 */
	@Deprecated(since = "7.0")
	public static RepeatableContainers of(
			Class<? extends Annotation> repeatable, @Nullable Class<? extends Annotation> container) {

		return explicitRepeatable(repeatable, container);
	}

	/**
	 * Create a {@link RepeatableContainers} instance that does not support any
	 * repeatable annotations.
	 * <p>Note, however, that {@link #plus(Class, Class)} may still be invoked on
	 * the {@code RepeatableContainers} instance returned from this method.
	 * <p>See the {@linkplain RepeatableContainers class-level javadoc} for examples
	 * and further details.
	 * @return a {@code RepeatableContainers} instance that does not support
	 * repeatable annotations
	 */
	public static RepeatableContainers none() {
		return NoRepeatableContainers.INSTANCE;
	}


	/**
	 * Standard {@link RepeatableContainers} implementation that searches using
	 * Java's {@link Repeatable @Repeatable} annotation.
	 */
	private static class StandardRepeatableContainers extends RepeatableContainers {

		private static final Object NONE = new Object();

		private static final StandardRepeatableContainers INSTANCE = new StandardRepeatableContainers();

		StandardRepeatableContainers() {
			super(null);
		}

		@Override
		Annotation @Nullable [] findRepeatedAnnotations(Annotation annotation) {
			Method method = getRepeatedAnnotationsMethod(annotation.annotationType());
			if (method != null) {
				return (Annotation[]) AnnotationUtils.invokeAnnotationMethod(method, annotation);
			}
			return super.findRepeatedAnnotations(annotation);
		}

		private static @Nullable Method getRepeatedAnnotationsMethod(Class<? extends Annotation> annotationType) {
			Object result = cache.computeIfAbsent(annotationType,
					StandardRepeatableContainers::computeRepeatedAnnotationsMethod);
			return (result != NONE ? (Method) result : null);
		}

		private static Object computeRepeatedAnnotationsMethod(Class<? extends Annotation> annotationType) {
			AttributeMethods methods = AttributeMethods.forAnnotationType(annotationType);
			Method method = methods.get(MergedAnnotation.VALUE);
			if (method != null) {
				Class<?> returnType = method.getReturnType();
				if (returnType.isArray()) {
					Class<?> componentType = returnType.componentType();
					if (Annotation.class.isAssignableFrom(componentType) &&
							componentType.isAnnotationPresent(Repeatable.class)) {
						return method;
					}
				}
			}
			return NONE;
		}
	}


	/**
	 * A single explicit mapping.
	 */
	private static class ExplicitRepeatableContainer extends RepeatableContainers {

		private final Class<? extends Annotation> repeatable;

		private final Class<? extends Annotation> container;

		private final Method valueMethod;

		ExplicitRepeatableContainer(@Nullable RepeatableContainers parent,
				Class<? extends Annotation> repeatable, @Nullable Class<? extends Annotation> container) {

			super(parent);
			Assert.notNull(repeatable, "Repeatable must not be null");
			if (container == null) {
				container = deduceContainer(repeatable);
			}
			Method valueMethod = AttributeMethods.forAnnotationType(container).get(MergedAnnotation.VALUE);
			try {
				if (valueMethod == null) {
					throw new NoSuchMethodException("No value method found");
				}
				Class<?> returnType = valueMethod.getReturnType();
				if (returnType.componentType() != repeatable) {
					throw new AnnotationConfigurationException(
							"Container type [%s] must declare a 'value' attribute for an array of type [%s]"
								.formatted(container.getName(), repeatable.getName()));
				}
			}
			catch (AnnotationConfigurationException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new AnnotationConfigurationException(
						"Invalid declaration of container type [%s] for repeatable annotation [%s]"
							.formatted(container.getName(), repeatable.getName()), ex);
			}
			this.repeatable = repeatable;
			this.container = container;
			this.valueMethod = valueMethod;
		}

		private Class<? extends Annotation> deduceContainer(Class<? extends Annotation> repeatable) {
			Repeatable annotation = repeatable.getAnnotation(Repeatable.class);
			Assert.notNull(annotation, () -> "Annotation type must be a repeatable annotation: " +
						"failed to resolve container type for " + repeatable.getName());
			return annotation.value();
		}

		@Override
		Annotation @Nullable [] findRepeatedAnnotations(Annotation annotation) {
			if (this.container.isAssignableFrom(annotation.annotationType())) {
				return (Annotation[]) AnnotationUtils.invokeAnnotationMethod(this.valueMethod, annotation);
			}
			return super.findRepeatedAnnotations(annotation);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (!super.equals(other)) {
				return false;
			}
			ExplicitRepeatableContainer otherErc = (ExplicitRepeatableContainer) other;
			return (this.container.equals(otherErc.container) && this.repeatable.equals(otherErc.repeatable));
		}

		@Override
		public int hashCode() {
			int hashCode = super.hashCode();
			hashCode = 31 * hashCode + this.container.hashCode();
			hashCode = 31 * hashCode + this.repeatable.hashCode();
			return hashCode;
		}
	}


	/**
	 * No repeatable containers.
	 */
	private static class NoRepeatableContainers extends RepeatableContainers {

		private static final NoRepeatableContainers INSTANCE = new NoRepeatableContainers();

		NoRepeatableContainers() {
			super(null);
		}
	}

}
