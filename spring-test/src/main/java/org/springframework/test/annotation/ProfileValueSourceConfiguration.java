/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code ProfileValueSourceConfiguration} is a class-level annotation which
 * is used to specify what type of {@link ProfileValueSource} to use when
 * retrieving <em>profile values</em> configured via the {@link IfProfileValue
 * &#064;IfProfileValue} annotation.
 *
 * <p>As of Spring Framework 4.0, this annotation may be used as a
 * <em>meta-annotation</em> to create custom <em>composed annotations</em>.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see ProfileValueSource
 * @see IfProfileValue
 * @see ProfileValueUtils
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ProfileValueSourceConfiguration {

	/**
	 * The type of {@link ProfileValueSource} to use when retrieving
	 * <em>profile values</em>.
	 *
	 * @see SystemProfileValueSource
	 */
	Class<? extends ProfileValueSource> value() default SystemProfileValueSource.class;

}
