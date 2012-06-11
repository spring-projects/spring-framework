/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * An {@linkplain Component @Component} annotation that indicates the annotated class
 * contains {@linkplain ExceptionHandler @ExceptionHandler} methods. Such methods
 * will be used in addition to {@code @ExceptionHandler} methods in
 * {@code @Controller}-annotated classes.
 *
 * <p>In order for the the annotation to detected, an instance of
 * {@code org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver}
 * is configured.
 *
 * <p>Classes with this annotation may use the {@linkplain Order @Order} annotation
 * or implement the {@link Ordered} interface to indicate the order in which they
 * should be used relative to other such annotated components. However, note that
 * the order is only for components registered through {@code @ExceptionResolver},
 * i.e. within an
 * {@code org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 *
 * @see org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ExceptionResolver {

}