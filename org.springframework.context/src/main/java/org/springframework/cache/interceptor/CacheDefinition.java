/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.cache.interceptor;

import java.util.Set;

/**
 * Interface describing Spring-compliant caching operation.
 * 
 * @author Costin Leau
 */
public interface CacheDefinition {

	/**
	 * Returns the name of this operation. Can be <tt>null</tt>.
	 * In case of Spring's declarative caching, the exposed name will be:
	 * <tt>fully qualified class name.method name</tt>. 
	 * 
	 * @return the operation name
	 */
	String getName();

	/**
	 * Returns the names of the cache against which this operation is performed.
	 * 
	 * @return names of the cache on which the operation is performed.
	 */
	Set<String> getCacheNames();

	/**
	 * Returns the SpEL expression conditioning the operation.  
	 * 
	 * @return operation condition (as SpEL expression).
	 */
	String getCondition();

	/**
	 * Returns the SpEL expression identifying the cache key.  
	 * 
	 * @return
	 */
	String getKey();

}
