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

package org.springframework.web.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.annotation.AliasFor;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@code @RequestScope} is a specialization of {@link Scope @Scope} for a
 * component whose lifecycle is bound to the current web request.
 *
 * <p>Specifically, {@code @RequestScope} is a <em>composed annotation</em> that
 * acts as a shortcut for {@code @Scope("request")} with the default
 * {@link #proxyMode} set to {@link ScopedProxyMode#TARGET_CLASS TARGET_CLASS}.
 *
 * <p>{@code @RequestScope} may be used as a meta-annotation to create custom
 * composed annotations.
 *
 * @author Sam Brannen
 * @since 4.3
 * @see SessionScope
 * @see ApplicationScope
 * @see org.springframework.context.annotation.Scope
 * @see org.springframework.web.context.WebApplicationContext#SCOPE_REQUEST
 * @see org.springframework.web.context.request.RequestScope
 * @see org.springframework.stereotype.Component
 * @see org.springframework.context.annotation.Bean
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Scope(WebApplicationContext.SCOPE_REQUEST)
public @interface RequestScope {

	/**
	 * Alias for {@link Scope#proxyMode}.
	 * <p>Defaults to {@link ScopedProxyMode#TARGET_CLASS}.
	 */
	@AliasFor(annotation = Scope.class)
	ScopedProxyMode proxyMode() default ScopedProxyMode.TARGET_CLASS;

}
