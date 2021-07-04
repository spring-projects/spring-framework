/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.aop.target.dynamic;

/**
 * Interface to be implemented by dynamic target objects,
 * which support reloading and optionally polling for updates.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @since 2.0
 */
public interface Refreshable {

	/**
	 * Refresh the underlying target object.
	 */
	void refresh();

	/**
	 * Return the number of actual refreshes since startup.
	 */
	long getRefreshCount();

	/**
	 * Return the last time an actual refresh happened (as timestamp).
	 */
	long getLastRefreshTime();

}
