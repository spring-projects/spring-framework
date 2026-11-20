/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.config;

/**
 * Configuration constants for internal sharing across subpackages.
 *
 * @author Juergen Hoeller
 * @since 4.1
 */
public abstract class TaskManagementConfigUtils {

	/**
	 * The bean name of the internally managed Scheduled annotation processor.
	 */
	public static final String SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME =
			"org.springframework.scheduling.config.internalScheduledAnnotationProcessor";

	/**
	 * The bean name of the internally managed Async annotation processor.
	 */
	public static final String ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME =
			"org.springframework.scheduling.config.internalAsyncAnnotationProcessor";

	/**
	 * The bean name of the internally managed AspectJ async execution aspect.
	 */
	public static final String ASYNC_EXECUTION_ASPECT_BEAN_NAME =
			"org.springframework.scheduling.config.internalAsyncExecutionAspect";

}
