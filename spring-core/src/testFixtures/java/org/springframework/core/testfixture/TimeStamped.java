/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.core.testfixture;

/**
 * This interface can be implemented by cacheable objects or cache entries,
 * to enable the freshness of objects to be checked.
 *
 * @author Rod Johnson
 */
public interface TimeStamped {

	/**
	 * Return the timestamp for this object.
	 * @return long the timestamp for this object,
	 * as returned by System.currentTimeMillis()
	 */
	long getTimeStamp();

}
