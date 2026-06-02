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

package org.springframework.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated component should be ordered before the specified target classes.
 *
 * <p>This annotation extends the {@code @AutoConfigureBefore} pattern from Spring Boot
 * to the core Spring framework, allowing for fine-grained control over the initialization
 * or processing order of components.</p>
 *
 * <p>For example, if class A must be initialized before class B, you can annotate class A
 * with {@code @DependsOnBefore(B.class)}.</p>
 *
 * <p>This annotation is primarily used in dependency management scenarios where
 * topological sorting is required to determine the correct order of execution.</p>
 *
 * @author Yongjun Hong
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DependsOnBefore {
	/**
	 * The target classes before which this component should be ordered.
	 */
	Class<?>[] value() default {};
}
