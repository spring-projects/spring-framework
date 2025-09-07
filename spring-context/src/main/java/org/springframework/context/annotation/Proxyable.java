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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Common annotation for suggesting a specific proxy type for a {@link Bean @Bean}
 * method or {@link org.springframework.stereotype.Component @Component} class,
 * overriding a globally configured default.
 *
 * <p>Only actually applying in case of a bean actually getting auto-proxied in
 * the first place. Actual auto-proxying is dependent on external configuration.
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @see org.springframework.aop.framework.autoproxy.AutoProxyUtils#PRESERVE_TARGET_CLASS_ATTRIBUTE
 * @see org.springframework.aop.framework.autoproxy.AutoProxyUtils#EXPOSED_INTERFACES_ATTRIBUTE
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Proxyable {

	/**
	 * Suggest a specific proxy type, either {@link ProxyType#INTERFACES} for
	 * a JDK dynamic proxy or {@link ProxyType#TARGET_CLASS} for a CGLIB proxy,
	 * overriding a globally configured default.
	 */
	ProxyType value() default ProxyType.DEFAULT;

	/**
	 * Suggest a JDK dynamic proxy with specific interfaces to expose, overriding
	 * a globally configured default.
	 * <p>Only taken into account if {@link #value()} is not {@link ProxyType#TARGET_CLASS}.
	 */
	Class<?>[] interfaces() default {};


}
