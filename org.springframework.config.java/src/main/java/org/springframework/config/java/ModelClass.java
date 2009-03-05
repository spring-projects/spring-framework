/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.config.java;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;


/**
 * Abstract representation of a class, free from java reflection. Base class used within the
 * internal JavaConfig metamodel for representing {@link Configuration} classes.
 * 
 * @author Chris Beams
 */
// TODO: Consider eliminating in favor of just ConfigurationClass
public class ModelClass implements BeanMetadataElement {

	private String name;
	private boolean isInterface;
	private String source;

	/**
	 * Creates a new and empty ModelClass instance.
	 */
	public ModelClass() {
	}

	/**
	 * Creates a new ModelClass instance
	 * 
	 * @param name fully-qualified name of the class being represented
	 */
	public ModelClass(String name) {
		this(name, false);
	}

	/**
	 * Creates a new ModelClass instance
	 * 
	 * @param name fully-qualified name of the class being represented
	 * @param isInterface whether the represented type is an interface
	 */
	public ModelClass(String name, boolean isInterface) {
		this.name = name;
		this.isInterface = isInterface;
	}

	/**
	 * Returns the fully-qualified name of this class.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the fully-qualified name of this class.
	 */
	public void setName(String className) {
		this.name = className;
	}

	/**
	 * Returns the non-qualified name of this class. Given com.acme.Foo, returns 'Foo'.
	 */
	public String getSimpleName() {
		return name == null ? null : ClassUtils.getShortName(name);
	}

	/**
	 * Returns whether the class represented by this ModelClass instance is an interface.
	 */
	public boolean isInterface() {
		return isInterface;
	}

	/**
	 * Signifies that this class is (true) or is not (false) an interface.
	 */
	public void setInterface(boolean isInterface) {
		this.isInterface = isInterface;
	}

	/**
	 * Returns a resource path-formatted representation of the .java file that declares this
	 * class
	 */
	// TODO: return type should be Object here.  Spring IDE will return a JDT representation...
	public String getSource() {
		return source;
	}

	/**
	 * Set the source location for this class. Must be a resource-path formatted string.
	 * 
	 * @param source resource path to the .java file that declares this class.
	 */
	public void setSource(Object source) {
		Assert.isInstanceOf(String.class, source);
		this.source = (String) source;
	}

	/**
	 * Given a ModelClass instance representing a class com.acme.Foo, this method will
	 * return
	 * 
	 * <pre>
	 * ModelClass: name=Foo
	 * </pre>
	 */
	@Override
	public String toString() {
		return String.format("%s: name=%s", getClass().getSimpleName(), getSimpleName());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + (isInterface ? 1231 : 1237);
		result = (prime * result) + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj == null)
			return false;

		if (getClass() != obj.getClass())
			return false;

		ModelClass other = (ModelClass) obj;
		if (isInterface != other.isInterface)
			return false;

		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;

		return true;
	}

}
