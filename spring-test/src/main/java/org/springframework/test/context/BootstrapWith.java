/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @BootstrapWith} is an annotation that can be applied to a test class
 * to define metadata that is used to determine how to bootstrap the
 * <em>Spring TestContext Framework</em>.
 *
 * <p>This annotation may also be used as a <em>meta-annotation</em> to create
 * custom <em>composed annotations</em>. Note, however, that a locally
 * declared {@code @BootstrapWith} annotation (i.e., one that is <em>directly
 * present</em> on the current test class) will override any meta-present
 * declarations of {@code @BootstrapWith}.
 *
 * <p>This annotation will be inherited from an enclosing test class by default.
 * See {@link NestedTestConfiguration @NestedTestConfiguration} for details.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see BootstrapContext
 * @see TestContextBootstrapper
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface BootstrapWith {

	/**
	 * The {@link TestContextBootstrapper} to use to bootstrap the <em>Spring
	 * TestContext Framework</em>.
	 */
	Class<? extends TestContextBootstrapper> value() default TestContextBootstrapper.class;

}
