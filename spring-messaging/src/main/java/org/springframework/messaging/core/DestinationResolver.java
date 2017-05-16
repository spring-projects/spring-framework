/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.messaging.core;

/**
 * Strategy for resolving a String destination name to an actual destination
 * of type {@code <D>}.
 *
 * @author Mark Fisher
 * @since 4.0
 */
@FunctionalInterface
public interface DestinationResolver<D> {

	/**
	 * Resolve the given destination name.
	 * @param name the destination name to resolve
	 * @return the resolved destination (never {@code null})
	 * @throws DestinationResolutionException if the specified destination
	 * wasn't found or wasn't resolvable for any other reason
	 */
	D resolveDestination(String name) throws DestinationResolutionException;

}
