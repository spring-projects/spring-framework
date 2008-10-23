/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.jmx.export.metadata;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.metadata.Attributes;
import org.springframework.util.Assert;

/**
 * Implementation of the <code>JmxAttributeSource</code> interface that
 * reads metadata via Spring's <code>Attributes</code> abstraction.
 *
 * <p>Typically used for reading in source-level attributes via
 * Commons Attributes.
 *
 * @author Rob Harrop
 * @since 1.2
 * @see org.springframework.metadata.Attributes
 * @see org.springframework.metadata.commons.CommonsAttributes
 */
public class AttributesJmxAttributeSource implements JmxAttributeSource, InitializingBean {

	/**
	 * Underlying Attributes implementation that we're using.
	 */
	private Attributes attributes;


	/**
	 * Create a new AttributesJmxAttributeSource.
	 * @see #setAttributes
	 */
	public AttributesJmxAttributeSource() {
	}

	/**
	 * Create a new AttributesJmxAttributeSource.
	 * @param attributes the Attributes implementation to use
	 * @see org.springframework.metadata.commons.CommonsAttributes
	 */
	public AttributesJmxAttributeSource(Attributes attributes) {
		if (attributes == null) {
			throw new IllegalArgumentException("Attributes is required");
		}
		this.attributes = attributes;
	}

	/**
	 * Set the Attributes implementation to use.
	 * @see org.springframework.metadata.commons.CommonsAttributes
	 */
	public void setAttributes(Attributes attributes) {
		this.attributes = attributes;
	}

	public void afterPropertiesSet() {
		if (this.attributes == null) {
			throw new IllegalArgumentException("'attributes' is required");
		}
	}


	/**
	 * If the specified class has a <code>ManagedResource</code> attribute,
	 * then it is returned. Otherwise returns null.
	 * @param clazz the class to read the attribute data from
	 * @return the attribute, or <code>null</code> if not found
	 * @throws InvalidMetadataException if more than one attribute exists
	 */
	public ManagedResource getManagedResource(Class clazz) {
		Assert.notNull(this.attributes, "'attributes' is required");
		Collection attrs = this.attributes.getAttributes(clazz, ManagedResource.class);
		if (attrs.isEmpty()) {
			return null;
		}
		else if (attrs.size() == 1) {
			return (ManagedResource) attrs.iterator().next();
		}
		else {
			throw new InvalidMetadataException("A Class can have only one ManagedResource attribute");
		}
	}

	/**
	 * If the specified method has a <code>ManagedAttribute</code> attribute,
	 * then it is returned. Otherwise returns null.
	 * @param method the method to read the attribute data from
	 * @return the attribute, or <code>null</code> if not found
	 * @throws InvalidMetadataException if more than one attribute exists,
	 * or if the supplied method does not represent a JavaBean property
	 */
	public ManagedAttribute getManagedAttribute(Method method) throws InvalidMetadataException {
		Assert.notNull(this.attributes, "'attributes' is required");
		PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method);
		if (pd == null) {
			throw new InvalidMetadataException(
					"The ManagedAttribute attribute is only valid for JavaBean properties: " +
					"use ManagedOperation for methods");
		}
		Collection attrs = this.attributes.getAttributes(method, ManagedAttribute.class);
		if (attrs.isEmpty()) {
			return null;
		}
		else if (attrs.size() == 1) {
			return (ManagedAttribute) attrs.iterator().next();
		}
		else {
			throw new InvalidMetadataException("A Method can have only one ManagedAttribute attribute");
		}
	}

	/**
	 * If the specified method has a <code>ManagedOperation</code> attribute,
	 * then it is returned. Otherwise return null.
	 * @param method the method to read the attribute data from
	 * @return the attribute, or <code>null</code> if not found
	 * @throws InvalidMetadataException if more than one attribute exists,
	 * or if the supplied method represents a JavaBean property
	 */
	public ManagedOperation getManagedOperation(Method method) {
		Assert.notNull(this.attributes, "'attributes' is required");
		PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method);
		if (pd != null) {
			throw new InvalidMetadataException(
					"The ManagedOperation attribute is not valid for JavaBean properties: " +
					"use ManagedAttribute instead");
		}
		Collection attrs = this.attributes.getAttributes(method, ManagedOperation.class);
		if (attrs.isEmpty()) {
			return null;
		}
		else if (attrs.size() == 1) {
			return (ManagedOperation) attrs.iterator().next();
		}
		else {
			throw new InvalidMetadataException("A Method can have only one ManagedAttribute attribute");
		}
	}

	/**
	 * If the specified method has <code>ManagedOperationParameter</code> attributes,
	 * then these are returned, otherwise a zero length array is returned.
	 * @param method the method to get the managed operation parameters for
	 * @return the array of ManagedOperationParameter objects
	 * @throws InvalidMetadataException if the number of ManagedOperationParameter
	 * attributes does not match the number of parameters in the method
	 */
	public ManagedOperationParameter[] getManagedOperationParameters(Method method)
			throws InvalidMetadataException {

		Assert.notNull(this.attributes, "'attributes' is required");
		Collection attrs = this.attributes.getAttributes(method, ManagedOperationParameter.class);
		if (attrs.size() == 0) {
			return new ManagedOperationParameter[0];
		}
		else if (attrs.size() != method.getParameterTypes().length) {
			throw new InvalidMetadataException(
					"Method [" + method + "] has an incorrect number of ManagedOperationParameters specified");
		}
		else {
			ManagedOperationParameter[] params = new ManagedOperationParameter[attrs.size()];
			for (Iterator it = attrs.iterator(); it.hasNext();) {
				ManagedOperationParameter param = (ManagedOperationParameter) it.next();
				if (param.getIndex() < 0 || param.getIndex() >= params.length) {
					throw new InvalidMetadataException(
							"ManagedOperationParameter index for [" + param.getName() + "] is out of bounds");
				}
				params[param.getIndex()] = param;
			}
			return params;
		}
	}

	/**
	 * If the specified has {@link ManagedNotification} attributes these are returned, otherwise
	 * a zero-length array is returned.
	 */
	public ManagedNotification[] getManagedNotifications(Class clazz) {
		Assert.notNull(this.attributes, "'attributes' is required");
		Collection attrs = this.attributes.getAttributes(clazz, ManagedNotification.class);
		return attrs.isEmpty() ? new ManagedNotification[0] : (ManagedNotification[]) attrs.toArray(new ManagedNotification[attrs.size()]);
	}
}
