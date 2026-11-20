/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop;

import org.jspecify.annotations.Nullable;

/**
 * A {@code TargetSource} is used to obtain the current "target" of
 * an AOP invocation, which will be invoked via reflection if no around
 * advice chooses to end the interceptor chain itself.
 *
 * <p>If a {@code TargetSource} is "static", it will always return
 * the same target, allowing optimizations in the AOP framework. Dynamic
 * target sources can support pooling, hot swapping, etc.
 *
 * <p>Application developers don't usually need to work with
 * {@code TargetSources} directly: this is an AOP framework interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public interface TargetSource extends TargetClassAware {

	/**
	 * Return the type of targets returned by this {@link TargetSource}.
	 * <p>Can return {@code null}, although certain usages of a {@code TargetSource}
	 * might just work with a predetermined target class.
	 * @return the type of targets returned by this {@link TargetSource}
	 */
	@Override
	@Nullable Class<?> getTargetClass();

	/**
	 * Will all calls to {@link #getTarget()} return the same object?
	 * <p>In that case, there will be no need to invoke {@link #releaseTarget(Object)},
	 * and the AOP framework can cache the return value of {@link #getTarget()}.
	 * <p>The default implementation returns {@code false}.
	 * @return {@code true} if the target is immutable
	 * @see #getTarget
	 */
	default boolean isStatic() {
		return false;
	}

	/**
	 * Return a target instance. Invoked immediately before the
	 * AOP framework calls the "target" of an AOP method invocation.
	 * @return the target object which contains the joinpoint,
	 * or {@code null} if there is no actual target instance
	 * @throws Exception if the target object can't be resolved
	 */
	@Nullable Object getTarget() throws Exception;

	/**
	 * Release the given target object obtained from the
	 * {@link #getTarget()} method, if any.
	 * <p>The default implementation is empty.
	 * @param target object obtained from a call to {@link #getTarget()}
	 * @throws Exception if the object can't be released
	 */
	default void releaseTarget(Object target) throws Exception {
	}

}
