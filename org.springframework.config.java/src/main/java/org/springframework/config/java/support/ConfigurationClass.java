/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.config.java.support;

import static java.lang.String.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.config.java.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.Assert;

import sun.security.x509.Extension;


/**
 * Abstract representation of a user-defined {@link Configuration @Configuration} class.
 * Includes a set of Bean methods, AutoBean methods, ExternalBean methods, ExternalValue
 * methods, etc. Includes all such methods defined in the ancestry of the class, in a
 * 'flattened-out' manner. Note that each BeanMethod representation does still contain
 * source information about where it was originally detected (for the purpose of tooling
 * with Spring IDE).
 * 
 * <p>
 * Like the rest of the {@link org.springframework.config.java.model model} package, this
 * class follows the fluent interface / builder pattern such that a model can be built up
 * easily by method chaining.
 * </p>
 * 
 * @author Chris Beams
 */
final class ConfigurationClass extends ModelClass {

	private String beanName;

	private int modifiers;

	private Configuration metadata;

	private HashSet<BeanMethod> methods = new HashSet<BeanMethod>();

	private HashSet<Annotation> pluginAnnotations = new HashSet<Annotation>();

	private ConfigurationClass declaringClass;

	public ConfigurationClass() {
	}

	// TODO: get rid of constructors used only for testing. put in testing util.
	/**
	 * Creates a new ConfigurationClass named <var>className.</var>
	 * 
	 * @param name fully-qualified Configuration class being represented
	 * 
	 * @see #setClassName(String)
	 */
	ConfigurationClass(String name) {
		this(name, null, defaultAnnotation(), 0);
	}

	ConfigurationClass(String name, Configuration metadata) {
		this(name, null, metadata, 0);
	}

	ConfigurationClass(String name, int modifiers) {
		this(name, null, defaultAnnotation(), modifiers);
	}

	private static Configuration defaultAnnotation() {
		@Configuration
		class Prototype {
		}
		return Prototype.class.getAnnotation(Configuration.class);
	}

	/**
	 * Creates a new ConfigurationClass object.
	 * 
	 * @param name Fully qualified name of the class being represented
	 * @param id Bean name/id (if any) of this configuration class. used only in the case of
	 *        XML integration where {@link Configuration} beans may have a user-specified
	 *        id.
	 * @param metadata Configuration annotation resident on this class. May be null
	 *        indicating that the user specified this class to be processed but failed to
	 *        properly annotate it.
	 * @param modifiers Per {@link java.lang.reflect.Modifier}
	 */
	public ConfigurationClass(String name, String id, Configuration metadata, int modifiers) {
		super(name);
		Assert.hasText(name, "Configuration class name must have text");

		setBeanName(id);
		setMetadata(metadata);
		setModifiers(modifiers);
	}

	public ConfigurationClass addMethod(BeanMethod method) {
		method.setDeclaringClass(this);
		methods.add(method);
		return this;
	}

	public String getBeanName() {
		return beanName == null ? getName() : beanName;
	}

	public ConfigurationClass setBeanName(String id) {
		this.beanName = id;
		return this;
	}

	public Set<BeanMethod> getMethods() {
		return methods;
	}

	public Annotation[] getPluginAnnotations() {
		return pluginAnnotations.toArray(new Annotation[pluginAnnotations.size()]);
	}

	/**
	 * Add a {@link Extension @Plugin}-annotated annotation to this configuration class.
	 * 
	 * @param pluginAnno type-level <code>Plugin</code> annotation
	 */
	public ConfigurationClass addPluginAnnotation(Annotation pluginAnno) {
		pluginAnnotations.add(pluginAnno);
		return this;
	}

	public ConfigurationClass setDeclaringClass(ConfigurationClass configurationClass) {
		this.declaringClass = configurationClass;
		return this;
	}

	public ConfigurationClass getDeclaringClass() {
		return declaringClass;
	}

	public int getModifiers() {
		return modifiers;
	}

	public ConfigurationClass setModifiers(int modifiers) {
		Assert.isTrue(modifiers >= 0, "modifiers must be non-negative");
		this.modifiers = modifiers;
		return this;
	}

	public Configuration getMetadata() {
		return this.metadata;
	}

	public ConfigurationClass setMetadata(Configuration configAnno) {
		this.metadata = configAnno;
		return this;
	}

	public void validate(ProblemReporter problemReporter) {

		// configuration classes must be annotated with @Configuration
		if (metadata == null)
			problemReporter.error(new NonAnnotatedConfigurationError());

		// a configuration class may not be final (CGLIB limitation)
		if (Modifier.isFinal(modifiers))
			problemReporter.error(new FinalConfigurationError());

		for (BeanMethod method : methods)
			method.validate(problemReporter);
	}

	@Override
	public String toString() {
		return format("%s; modifiers=%d; methods=%s", super.toString(), modifiers, methods);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((declaringClass == null) ? 0 : declaringClass.hashCode());
		result = prime * result + ((beanName == null) ? 0 : beanName.hashCode());
		result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
		result = prime * result + ((methods == null) ? 0 : methods.hashCode());
		result = prime * result + modifiers;
		result = prime * result + ((pluginAnnotations == null) ? 0 : pluginAnnotations.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConfigurationClass other = (ConfigurationClass) obj;
		if (declaringClass == null) {
			if (other.declaringClass != null)
				return false;
		} else if (!declaringClass.equals(other.declaringClass))
			return false;
		if (beanName == null) {
			if (other.beanName != null)
				return false;
		} else if (!beanName.equals(other.beanName))
			return false;
		if (metadata == null) {
			if (other.metadata != null)
				return false;
		} else if (!metadata.equals(other.metadata))
			return false;
		if (methods == null) {
			if (other.methods != null)
				return false;
		} else if (!methods.equals(other.methods))
			return false;
		if (modifiers != other.modifiers)
			return false;
		if (pluginAnnotations == null) {
			if (other.pluginAnnotations != null)
				return false;
		} else if (!pluginAnnotations.equals(other.pluginAnnotations))
			return false;
		return true;
	}


	/** Configuration classes must be annotated with {@link Configuration @Configuration}. */
	public class NonAnnotatedConfigurationError extends Problem {
		public NonAnnotatedConfigurationError() {
			super(format("%s was provided as a Java Configuration class but was not annotated with @%s. "
			           + "Update the class definition to continue.",
			             getSimpleName(), Configuration.class.getSimpleName()),
			      new Location(new FileSystemResource("/dev/null"))
			);
		}

	}


	/** Configuration classes must be non-final to accommodate CGLIB subclassing. */
	public class FinalConfigurationError extends Problem {
		public FinalConfigurationError() {
			super(format("@%s class may not be final. Remove the final modifier to continue.",
			             Configuration.class.getSimpleName()),
			      new Location(new FileSystemResource("/dev/null"))
			);
		}
	}

}
