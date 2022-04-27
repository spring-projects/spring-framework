/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.jmx.export.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method-level annotation that indicates to expose a given method as a JMX operation,
 * corresponding to the {@link org.springframework.jmx.export.metadata.ManagedOperation}
 * attribute.
 *
 * <p>Only valid when used on a method that is not a JavaBean getter or setter.
 *
 * @author Rob Harrop
 * @since 1.2
 * @see org.springframework.jmx.export.metadata.ManagedOperation
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ManagedOperation {

	String description() default "";

	int currencyTimeLimit() default -1;

}
