/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @TestPropertySources} is a container for one or more
 * {@link TestPropertySource @TestPropertySource} declarations.
 *
 * <p>Note, however, that use of the {@code @TestPropertySources} container is
 * completely optional since {@code @TestPropertySource} is a
 * {@linkplain java.lang.annotation.Repeatable repeatable} annotation.
 *
 * @author Anatoliy Korovin
 * @author Sam Brannen
 * @since 5.2
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface TestPropertySources {

	/**
	 * An array of one or more {@link TestPropertySource @TestPropertySource}
	 * declarations.
	 */
	TestPropertySource[] value();

}
