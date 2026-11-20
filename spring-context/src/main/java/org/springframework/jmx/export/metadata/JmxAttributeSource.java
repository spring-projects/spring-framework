/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.metadata;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

/**
 * Interface used by the {@code MetadataMBeanInfoAssembler} to
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
	 * Implementations should return an instance of {@link ManagedResource}
	 * if the supplied {@code Class} has the corresponding metadata.
	 * @param clazz the class to read the resource data from
	 * @return the resource, or {@code null} if not found
	 * @throws InvalidMetadataException in case of invalid metadata
	 */
	@Nullable ManagedResource getManagedResource(Class<?> clazz) throws InvalidMetadataException;

	/**
	 * Implementations should return an instance of {@link ManagedAttribute}
	 * if the supplied {@code Method} has the corresponding metadata.
	 * @param method the method to read the attribute data from
	 * @return the attribute, or {@code null} if not found
	 * @throws InvalidMetadataException in case of invalid metadata
	 */
	@Nullable ManagedAttribute getManagedAttribute(Method method) throws InvalidMetadataException;

	/**
	 * Implementations should return an instance of {@link ManagedMetric}
	 * if the supplied {@code Method} has the corresponding metadata.
	 * @param method the method to read the metric data from
	 * @return the metric, or {@code null} if not found
	 * @throws InvalidMetadataException in case of invalid metadata
	 */
	@Nullable ManagedMetric getManagedMetric(Method method) throws InvalidMetadataException;

	/**
	 * Implementations should return an instance of {@link ManagedOperation}
	 * if the supplied {@code Method} has the corresponding metadata.
	 * @param method the method to read the operation data from
	 * @return the operation, or {@code null} if not found
	 * @throws InvalidMetadataException in case of invalid metadata
	 */
	@Nullable ManagedOperation getManagedOperation(Method method) throws InvalidMetadataException;

	/**
	 * Implementations should return an array of {@link ManagedOperationParameter
	 * ManagedOperationParameters} if the supplied {@code Method} has the corresponding
	 * metadata.
	 * @param method the {@code Method} to read the metadata from
	 * @return the parameter information, or an empty array if no metadata is found
	 * @throws InvalidMetadataException in case of invalid metadata
	 */
	@Nullable ManagedOperationParameter[] getManagedOperationParameters(Method method) throws InvalidMetadataException;

	/**
	 * Implementations should return an array of {@link ManagedNotification ManagedNotifications}
	 * if the supplied {@code Class} has the corresponding metadata.
	 * @param clazz the {@code Class} to read the metadata from
	 * @return the notification information, or an empty array if no metadata is found
	 * @throws InvalidMetadataException in case of invalid metadata
	 */
	@Nullable ManagedNotification[] getManagedNotifications(Class<?> clazz) throws InvalidMetadataException;

}
