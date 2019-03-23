/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.jms.config;

/**
 * Configuration constants for internal sharing across subpackages.
 *
 * @author Juergen Hoeller
 * @since 4.1
 */
public abstract class JmsListenerConfigUtils {

	/**
	 * The bean name of the internally managed JMS listener annotation processor.
	 */
	public static final String JMS_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME =
			"org.springframework.jms.config.internalJmsListenerAnnotationProcessor";

	/**
	 * The bean name of the internally managed JMS listener endpoint registry.
	 */
	public static final String JMS_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME =
			"org.springframework.jms.config.internalJmsListenerEndpointRegistry";

}
