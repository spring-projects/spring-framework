/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.core.type.MethodMetadata;
import org.springframework.lang.Nullable;

/**
 * Represents a {@link Configuration @Configuration} class method annotated with
 * {@link Bean @Bean}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see ConfigurationClass
 * @see ConfigurationClassParser
 * @see ConfigurationClassBeanDefinitionReader
 */
final class BeanMethod extends ConfigurationMethod {

	BeanMethod(MethodMetadata metadata, ConfigurationClass configurationClass) {
		super(metadata, configurationClass);
	}


	@Override
	@SuppressWarnings("NullAway")
	public void validate(ProblemReporter problemReporter) {
		if (getMetadata().getAnnotationAttributes(Autowired.class.getName()) != null) {
			// declared as @Autowired: semantic mismatch since @Bean method arguments are autowired
			// in any case whereas @Autowired methods are setter-like methods on the containing class
			problemReporter.error(new AutowiredDeclaredMethodError());
		}

		if ("void".equals(getMetadata().getReturnTypeName())) {
			// declared as void: potential misuse of @Bean, maybe meant as init method instead?
			problemReporter.error(new VoidDeclaredMethodError());
		}

		if (getMetadata().isStatic()) {
			// static @Bean methods have no further constraints to validate -> return immediately
			return;
		}

		Map<String, Object> attributes =
				getConfigurationClass().getMetadata().getAnnotationAttributes(Configuration.class.getName());
		if (attributes != null && (Boolean) attributes.get("proxyBeanMethods") && !getMetadata().isOverridable()) {
			// instance @Bean methods within @Configuration classes must be overridable to accommodate CGLIB
			problemReporter.error(new NonOverridableMethodError());
		}
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof BeanMethod that &&
				this.configurationClass.equals(that.configurationClass) &&
				getLocalMethodIdentifier(this.metadata).equals(getLocalMethodIdentifier(that.metadata))));
	}

	@Override
	public int hashCode() {
		return this.configurationClass.hashCode() * 31 + getLocalMethodIdentifier(this.metadata).hashCode();
	}

	@Override
	public String toString() {
		return "BeanMethod: " + this.metadata;
	}


	private static String getLocalMethodIdentifier(MethodMetadata metadata) {
		String metadataString = metadata.toString();
		int index = metadataString.indexOf(metadata.getDeclaringClassName());
		return (index >= 0 ? metadataString.substring(index + metadata.getDeclaringClassName().length()) :
				metadataString);
	}


	private class AutowiredDeclaredMethodError extends Problem {

		AutowiredDeclaredMethodError() {
			super("@Bean method '%s' must not be declared as autowired; remove the method-level @Autowired annotation."
					.formatted(getMetadata().getMethodName()), getResourceLocation());
		}
	}


	private class VoidDeclaredMethodError extends Problem {

		VoidDeclaredMethodError() {
			super("@Bean method '%s' must not be declared as void; change the method's return type or its annotation."
					.formatted(getMetadata().getMethodName()), getResourceLocation());
		}
	}


	private class NonOverridableMethodError extends Problem {

		NonOverridableMethodError() {
			super("@Bean method '%s' must not be private or final; change the method's modifiers to continue."
					.formatted(getMetadata().getMethodName()), getResourceLocation());
		}
	}

}
