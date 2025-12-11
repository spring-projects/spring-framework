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

package org.springframework.web.reactive.function.server.cache

/**
 * Annotation indicating that the result of invoking a *suspend method* can be cached
 * for the lifespan of the underlying web request coroutine.
 *
 * Example:
 *
 * ```
 * class MyServiceBean {
 *
 * 	@CoRequestCacheable(key = "#userName")
 * 	suspend fun fetchUserAgeFromDownstreamService(userName: String, authHeader: String): Int {
 * 		// prepare request and fetch the user info
 * 		return userInfo.age
 * 	}
 *
 * }
 * ```
 *
 * Each time an advised suspend method is invoked, caching behavior will be applied,
 * checking whether the method has been already invoked for the given arguments *within the same web request execution*.
 * A sensible default simply uses the method parameters to compute the key, but
 * a SpEL expression can be provided via the [key] attribute.
 *
 * If no value is found in the cache for the computed key, the target method
 * will be invoked and the returned value will be stored in the coroutine context.
 *
 * Note that breaking
 * [structured concurrency](https://kotlinlang.org/docs/coroutines-basics.html#coroutine-scope-and-structured-concurrency)
 * by invoking the annotated method in a coroutine scope not tied to the web request, will prevent any caching behaviour.
 *
 * @author Angelo Bracaglia
 * @since 7.0
 * @see EnableCoRequestCaching
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class CoRequestCacheable(

	/**
	 * Spring Expression Language (SpEL) expression for computing the key dynamically.
	 *
	 * The default value is `""`, meaning all method parameters are considered as a key.
	 *
	 * Method arguments can be accessed by index. For instance the second argument
	 * can be accessed via `#p1` or `#a1`.
	 * Arguments can also be accessed by name if that information is available.
	 */
	val key: String = ""
)
