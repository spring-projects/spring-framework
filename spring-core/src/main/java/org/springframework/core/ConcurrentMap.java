/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core;

import java.util.Map;

/**
 * Common interface for a concurrent Map, as exposed by
 * {@link CollectionFactory#createConcurrentMap}. Mirrors
 * {@link java.util.concurrent.ConcurrentMap}, allowing to be backed by a
 * JDK ConcurrentHashMap as well as a backport-concurrent ConcurrentHashMap.
 *
 * <p>Check out the {@link java.util.concurrent.ConcurrentMap ConcurrentMap javadoc}
 * for details on the interface's methods.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @deprecated as of Spring 3.0, since standard {@link java.util.concurrent.ConcurrentMap}
 * is available on Java 5+ anyway
 */
@SuppressWarnings("rawtypes")
@Deprecated
public interface ConcurrentMap extends Map {

	Object putIfAbsent(Object key, Object value);

	boolean remove(Object key, Object value);

	boolean replace(Object key, Object oldValue, Object newValue);

	Object replace(Object key, Object value);

}
