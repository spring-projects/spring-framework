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

package org.springframework.web.service.registry;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Annotation to mark an HTTP Service interface as a candidate client proxy creation.
 * Supported through the import of an {@link AbstractClientHttpServiceRegistrar}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 * @see AbstractClientHttpServiceRegistrar
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HttpServiceClient {

	/**
	 * An alias for {@link #group()}.
	 */
	@AliasFor("group")
	String value() default HttpServiceGroup.DEFAULT_GROUP_NAME;

	/**
	 * The name of the HTTP Service group for this client.
	 * <p>By default, this is {@link HttpServiceGroup#DEFAULT_GROUP_NAME}.
	 */
	@AliasFor("value")
	String group() default HttpServiceGroup.DEFAULT_GROUP_NAME;

}
