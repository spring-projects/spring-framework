/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

/**
 * Strategy interface for Bean Override processing, which creates
 * {@link BeanOverrideHandler} instances that drive how target beans are
 * overridden.
 *
 * <p>At least one composed annotation that is meta-annotated with
 * {@link BeanOverride @BeanOverride} must be a companion of this processor and
 * may optionally provide annotation attributes that can be used to configure the
 * {@code BeanOverrideHandler}.
 *
 * <p>Implementations are required to have a no-argument constructor and be
 * stateless.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.2
 */
public interface BeanOverrideProcessor {

	/**
	 * Create a {@link BeanOverrideHandler} for the given annotated field.
	 * <p>This method will only be invoked when a {@link BeanOverride @BeanOverride}
	 * annotation is declared on a field &mdash; for example, if the supplied field
	 * is annotated with {@code @MockitoBean}.
	 * @param overrideAnnotation the composed annotation that declares the
	 * {@code @BeanOverride} annotation which registers this processor
	 * @param testClass the test class to process
	 * @param field the annotated field
	 * @return the {@code BeanOverrideHandler} that should handle the given field
	 * @see #createHandlers(Annotation, Class)
	 */
	BeanOverrideHandler createHandler(Annotation overrideAnnotation, Class<?> testClass, Field field);

	/**
	 * Create a list of {@link BeanOverrideHandler} instances for the given override
	 * annotation and test class.
	 * <p>This method will only be invoked when a {@link BeanOverride @BeanOverride}
	 * annotation is declared at the type level &mdash; for example, if the supplied
	 * test class is annotated with {@code @MockitoBean}.
	 * <p>Note that the test class may not be directly annotated with the override
	 * annotation. For example, the override annotation may have been declared
	 * on an interface, superclass, or enclosing class within the test class
	 * hierarchy.
	 * <p>The default implementation returns an empty list, signaling that this
	 * {@code BeanOverrideProcessor} does not support type-level {@code @BeanOverride}
	 * declarations. Can be overridden by concrete implementations to support
	 * type-level use cases.
	 * @param overrideAnnotation the composed annotation that declares the
	 * {@code @BeanOverride} annotation which registers this processor
	 * @param testClass the test class to process
	 * @return the list of {@code BeanOverrideHandlers} for the annotated class
	 * @since 6.2.2
	 * @see #createHandler(Annotation, Class, Field)
	 */
	default List<BeanOverrideHandler> createHandlers(Annotation overrideAnnotation, Class<?> testClass) {
		return Collections.emptyList();
	}

}
