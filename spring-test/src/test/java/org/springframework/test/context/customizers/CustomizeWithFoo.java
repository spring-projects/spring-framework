/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.customizers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.test.context.ContextCustomizerFactories;

/**
 * @author Sam Brannen
 * @since 6.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ContextCustomizerFactories(FooContextCustomizerFactory.class)
@interface CustomizeWithFoo {
}
