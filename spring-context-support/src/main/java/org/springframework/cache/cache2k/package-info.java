/**
 * Support classes for <a href="https://cache2k.org">cache2k</a> open source
 * cache implementation allowing to set up cache2k caches within Spring's cache abstraction.
 *
 * <p>Note: cache2k has support for JSR107/JCache and can be used via
 * Spring's support in {@code org.springframework.cache.jcache} as well. Using
 * cache2k direct support instead of JCache reduces memory overhead and enhances overall
 * caching performance.
 */
@NonNullApi
@NonNullFields
package org.springframework.cache.cache2k;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;