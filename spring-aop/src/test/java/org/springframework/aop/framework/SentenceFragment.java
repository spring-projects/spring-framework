/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @SentenceFragment} is used to configure a sentence fragment for use
 * with JUnit Jupiter's
 * {@link org.junit.jupiter.api.DisplayNameGenerator.IndicativeSentences}
 * {@code DisplayNameGenerator}.
 *
 * @author Sam Brannen
 * @since 7.0
 * @see SentenceFragmentDisplayNameGenerator
 * @see org.junit.jupiter.api.DisplayName
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@interface SentenceFragment {

	String value();

}
