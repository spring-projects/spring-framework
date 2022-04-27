/*
 * Copyright 2002-2020 the original author or authors.
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
 * Method-level annotation that indicates to expose a given bean property as a
 * JMX attribute, corresponding to the
 * {@link org.springframework.jmx.export.metadata.ManagedAttribute}.
 *
 * <p>Only valid when used on a JavaBean getter or setter.
 *
 * @author Rob Harrop
 * @since 1.2
 * @see org.springframework.jmx.export.metadata.ManagedAttribute
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ManagedAttribute {

	/**
	 * Set the default value for the attribute in a {@link javax.management.Descriptor}.
	 */
	String defaultValue() default "";

	/**
	 * Set the description for the attribute in a {@link javax.management.Descriptor}.
	 */
	String description() default "";

	/**
	 * Set the currency time limit field in a {@link javax.management.Descriptor}.
	 */
	int currencyTimeLimit() default -1;

	/**
	 * Set the persistPolicy field in a {@link javax.management.Descriptor}.
	 */
	String persistPolicy() default "";

	/**
	 * Set the persistPeriod field in a {@link javax.management.Descriptor}.
	 */
	int persistPeriod() default -1;

}
