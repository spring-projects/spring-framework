package org.springframework.test.context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the {@link org.springframework.context.ApplicationContext}
 * associated with the test should be retained between test classes and
 * not paused when it is reused from the context cache.
 *
 * <p>This is an opt-in mechanism intended for tests that rely on expensive
 * {@link org.springframework.context.SmartLifecycle} components whose
 * stop/start cycles significantly impact test performance.
 *
 * <p>When a test class is annotated with {@code @RetainApplicationContext},
 * the application context will <em>not</em> be paused or restarted between
 * test classes, even if the context would normally be considered unused.
 *
 * @since 7.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RetainApplicationContext {
}
