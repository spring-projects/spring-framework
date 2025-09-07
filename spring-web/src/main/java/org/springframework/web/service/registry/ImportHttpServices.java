/*
 * Copyright 2012-present the original author or authors.
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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Annotation to declare HTTP Service types (interfaces with
 * {@link HttpExchange @HttpExchange} methods) for which to create client proxies,
 * and have those proxies registered as beans.
 *
 * <p>This is a repeatable annotation that is expected on
 * {@link org.springframework.context.annotation.Configuration @Configuration}
 * classes. Each annotation is associated with an {@link HttpServiceGroup}
 * identified by name through the {@link #group()} attribute.
 *
 * <p>The HTTP Services for each group can be listed via {@link #types()}, or
 * detected via {@link #basePackageClasses()} or {@link #basePackages()}.
 *
 * <p>An application can autowire HTTP Service proxy beans, or autowire the
 * {@link HttpServiceProxyRegistry} from which to obtain proxies.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @since 7.0
 * @see Container
 * @see AbstractHttpServiceRegistrar
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(ImportHttpServices.Container.class)
@Import(ImportHttpServiceRegistrar.class)
public @interface ImportHttpServices {

	/**
	 * An alias for {@link #types()}.
	 */
	@AliasFor("types")
	Class<?>[] value() default {};

	/**
	 * A list of HTTP Service types to include in the group.
	 */
	@AliasFor("value")
	Class<?>[] types() default {};

	/**
	 * The name of the HTTP Service group.
	 * <p>If not specified, declared HTTP Services are grouped under the
	 * {@link HttpServiceGroup#DEFAULT_GROUP_NAME}.
	 */
	String group() default HttpServiceGroup.DEFAULT_GROUP_NAME;

	/**
	 * Detect HTTP Services in the packages of the specified classes, looking
	 * for interfaces with type or method {@link HttpExchange} annotations.
	 * <p>The performed scan, however, filters out interfaces annotated with
	 * {@link HttpServiceClient} that are instead supported by
	 * {@link AbstractClientHttpServiceRegistrar}.
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * Variant of {@link #basePackageClasses()} with a list of packages
	 * specified by package name.
	 */
	String[] basePackages() default {};

	/**
	 * Specify the type of client to use for the group.
	 * <p>By default, this is {@link HttpServiceGroup.ClientType#UNSPECIFIED}
	 * in which case {@code RestClient} is used, but this default can be changed
	 * via {@link AbstractHttpServiceRegistrar#setDefaultClientType}.
	 */
	HttpServiceGroup.ClientType clientType() default HttpServiceGroup.ClientType.UNSPECIFIED;


	/**
	 * Container annotation that is necessary for the repeatable
	 * {@link ImportHttpServices @ImportHttpServices} annotation, but does not
	 * need to be declared in application code.
	 */
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Import(ImportHttpServiceRegistrar.class)
	@interface Container {

		ImportHttpServices[] value();
	}

}
