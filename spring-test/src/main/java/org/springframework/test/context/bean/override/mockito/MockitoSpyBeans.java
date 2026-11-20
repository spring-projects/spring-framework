/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container for {@link MockitoSpyBean @MockitoSpyBean} annotations which allows
 * {@code @MockitoSpyBean} to be used as a {@linkplain java.lang.annotation.Repeatable
 * repeatable annotation} at the type level &mdash; for example, on test classes
 * or interfaces implemented by test classes.
 *
 * @author Sam Brannen
 * @since 6.2.3
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MockitoSpyBeans {

	MockitoSpyBean[] value();

}
