/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.aot.hint.annotation.Reflective;

/**
 * Annotation that marks a method as a candidate for <i>asynchronous</i> execution.
 *
 * <p>Can also be used at the type level, in which case all the type's methods are
 * considered as asynchronous. Note, however, that {@code @Async} is not supported
 * on methods declared within a
 * {@link org.springframework.context.annotation.Configuration @Configuration} class.
 *
 * <p>In terms of target method signatures, any parameter types are supported.
 * However, the return type is constrained to either {@code void} or
 * {@link java.util.concurrent.Future}. In the latter case, you may declare the
 * more specific {@link java.util.concurrent.CompletableFuture} type which allows
 * for richer interaction with the asynchronous task and for immediate composition
 * with further processing steps.
 *
 * <p>A {@code Future} handle returned from the proxy will be an actual asynchronous
 * {@code (Completable)Future} that can be used to track the result of the
 * asynchronous method execution. However, since the target method needs to implement
 * the same signature, it will have to return a temporary {@code Future} handle that
 * just passes a value after computation in the execution thread: typically through
 * {@link java.util.concurrent.CompletableFuture#completedFuture(Object)}. The
 * provided value will be exposed to the caller through the actual asynchronous
 * {@code Future} handle at runtime.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.0
 * @see AnnotationAsyncExecutionInterceptor
 * @see AsyncAnnotationAdvisor
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Reflective
public @interface Async {

	/**
	 * A qualifier value for the specified asynchronous operation(s).
	 * <p>May be used to determine the target executor to be used when executing
	 * the asynchronous operation(s), matching the qualifier value (or the bean
	 * name) of a specific {@link java.util.concurrent.Executor Executor} or
	 * {@link org.springframework.core.task.TaskExecutor TaskExecutor}
	 * bean definition.
	 * <p>When specified in a class-level {@code @Async} annotation, indicates that the
	 * given executor should be used for all methods within the class. Method-level use
	 * of {@code Async#value} always overrides any qualifier value configured at
	 * the class level.
	 * <p>The qualifier value will be resolved dynamically if supplied as a SpEL
	 * expression (for example, {@code "#{environment['myExecutor']}"}) or a
	 * property placeholder (for example, {@code "${my.app.myExecutor}"}).
	 * @since 3.1.2
	 */
	String value() default "";

}
