/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.core.annotation;

import org.springframework.core.NestedRuntimeException;

/**
 * Thrown by {@link AnnotationUtils} and <em>synthesized annotations</em>
 * if an annotation is improperly configured.
 *
 * @author Sam Brannen
 * @since 4.2
 * @see AnnotationUtils
 * @see SynthesizedAnnotation
 */
@SuppressWarnings("serial")
public class AnnotationConfigurationException extends NestedRuntimeException {

	/**
	 * Construct a new {@code AnnotationConfigurationException} with the
	 * supplied message.
	 * @param message the detail message
	 */
	public AnnotationConfigurationException(String message) {
		super(message);
	}

	/**
	 * Construct a new {@code AnnotationConfigurationException} with the
	 * supplied message and cause.
	 * @param message the detail message
	 * @param cause the root cause
	 */
	public AnnotationConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

}
