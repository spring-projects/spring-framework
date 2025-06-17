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
