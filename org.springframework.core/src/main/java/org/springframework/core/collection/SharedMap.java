/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.core.collection;

import java.util.Map;

/**
 * A simple subinterface of {@link Map} that exposes a mutex that application code can synchronize on.
 * <p>
 * Expected to be implemented by Maps that are backed by shared objects that require synchronization between multiple
 * threads. An example would be the HTTP session map.
 * 
 * @author Keith Donald
 */
public interface SharedMap<K, V> extends Map<K, V> {

	/**
	 * Returns the shared mutex that may be synchronized on using a synchronized block. The returned mutex is guaranteed
	 * to be non-null.
	 * 
	 * Example usage:
	 * 
	 * <pre>
	 * synchronized (sharedMap.getMutex()) {
	 * 	// do synchronized work
	 * }
	 * </pre>
	 * 
	 * @return the mutex
	 */
	public Object getMutex();
}