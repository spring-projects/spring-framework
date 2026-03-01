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
 * Indicates that the annotated component should be ordered after the specified target classes.
 *
 * <p>This annotation is an extension of the {@code @AutoConfigureAfter} pattern from Spring Boot,
 * adapted for use in the core Spring framework. It allows developers to specify that a component
 * must be initialized or processed after certain other components.</p>
 *
 * <p>For example, if class A depends on class B being initialized first, class A can be annotated
 * with {@code @DependsOnAfter(B.class)} to enforce this order.</p>
 *
 * <p>This annotation is particularly useful in scenarios where the initialization order of
 * components affects application behavior, and topological sorting is required to resolve
 * dependencies.</p>
 *
 * @author Yongjun Hong
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DependsOnAfter {
	/**
	 * The target classes after which this component should be ordered.
	 */
	Class<?>[] value() default {};
}
