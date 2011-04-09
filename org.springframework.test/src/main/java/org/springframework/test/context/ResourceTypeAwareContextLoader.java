/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.test.context;

/**
 * Extension of the {@link ContextLoader} API for context loaders that
 * are aware of the type of context configuration resources that they
 * support.
 * <p>Prior to Spring 3.1, context loaders supported only String-based
 * resource locations; as of Spring 3.1 context loaders may choose to
 * support either String-based or Class-based resources (but not both).
 * <p>If a context loader does not implement this interface it is assumed
 * that the loader supports String-based resource locations.
 *
 * @author Sam Brannen
 * @since 3.1
 */
public interface ResourceTypeAwareContextLoader extends ContextLoader {

	/**
	 * Enumeration of context configuration resource types that a given
	 * <code>ContextLoader</code> can support.
	 * <p>The enum constants have a one-to-one correlation to attributes
	 * of the {@link ContextConfiguration} annotation.
	 */
	public static enum ResourceType {

		/**
		 * String-based resource locations.
		 * @see ContextConfiguration#locations
		 * @see ContextConfiguration#value
		 */
		LOCATIONS,

		/**
		 * Configuration class resources.
		 * @see ContextConfiguration#classes
		 */
		CLASSES;

	};


	/**
	 * @return the context configuration resource type supported by this ContextLoader
	 */
	ResourceType getResourceType();

}
