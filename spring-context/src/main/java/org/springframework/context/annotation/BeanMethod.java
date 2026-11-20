/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.core.type.MethodMetadata;

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
	@SuppressWarnings("NullAway") // Reflection
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

		Map<String, @Nullable Object> attributes =
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
