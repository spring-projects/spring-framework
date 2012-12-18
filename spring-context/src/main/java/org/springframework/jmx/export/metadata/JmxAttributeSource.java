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

package org.springframework.jmx.export.metadata;

import java.lang.reflect.Method;

/**
 * Interface used by the <code>MetadataMBeanInfoAssembler</code> to
 * read source-level metadata from a managed resource's class.
 *
 * @author Rob Harrop
 * @author Jennifer Hickey
 * @since 1.2
 * @see org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler#setAttributeSource
 * @see org.springframework.jmx.export.MBeanExporter#setAssembler
 */
public interface JmxAttributeSource {

	/**
	 * Implementations should return an instance of <code>ManagedResource</code>
	 * if the supplied <code>Class</code> has the appropriate metadata.
	 * Otherwise should return <code>null</code>.
	 * @param clazz the class to read the attribute data from
	 * @return the attribute, or <code>null</code> if not found
	 * @throws InvalidMetadataException in case of invalid attributes
	 */
	ManagedResource getManagedResource(Class<?> clazz) throws InvalidMetadataException;

	/**
	 * Implementations should return an instance of <code>ManagedAttribute</code>
	 * if the supplied <code>Method</code> has the corresponding metadata.
	 * Otherwise should return <code>null</code>.
	 * @param method the method to read the attribute data from
	 * @return the attribute, or <code>null</code> if not found
	 * @throws InvalidMetadataException in case of invalid attributes
	 */
	ManagedAttribute getManagedAttribute(Method method) throws InvalidMetadataException;

	/**
	 * Implementations should return an instance of <code>ManagedMetric</code>
	 * if the supplied <code>Method</code> has the corresponding metadata.
	 * Otherwise should return <code>null</code>.
	 * @param method the method to read the attribute data from
	 * @return the metric, or <code>null</code> if not found
	 * @throws InvalidMetadataException in case of invalid attributes
	 */
	ManagedMetric getManagedMetric(Method method) throws InvalidMetadataException;

	/**
	 * Implementations should return an instance of <code>ManagedOperation</code>
	 * if the supplied <code>Method</code> has the corresponding metadata.
	 * Otherwise should return <code>null</code>.
	 * @param method the method to read the attribute data from
	 * @return the attribute, or <code>null</code> if not found
	 * @throws InvalidMetadataException in case of invalid attributes
	 */
	ManagedOperation getManagedOperation(Method method) throws InvalidMetadataException;

	/**
	 * Implementations should return an array of <code>ManagedOperationParameter</code>
	 * if the supplied <code>Method</code> has the corresponding metadata. Otherwise
	 * should return an empty array if no metadata is found.
	 * @param method the <code>Method</code> to read the metadata from
	 * @return the parameter information.
	 * @throws InvalidMetadataException in the case of invalid attributes.
	 */
	ManagedOperationParameter[] getManagedOperationParameters(Method method) throws InvalidMetadataException;

	/**
	 * Implementations should return an array of {@link ManagedNotification ManagedNotifications}
	 * if the supplied the <code>Class</code> has the corresponding metadata. Otherwise
	 * should return an empty array.
	 * @param clazz the <code>Class</code> to read the metadata from
	 * @return the notification information
	 * @throws InvalidMetadataException in the case of invalid metadata
	 */
	ManagedNotification[] getManagedNotifications(Class<?> clazz) throws InvalidMetadataException;



}
