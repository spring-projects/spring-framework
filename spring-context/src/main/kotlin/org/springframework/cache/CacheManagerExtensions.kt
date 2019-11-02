package org.springframework.cache

/**
 * Extension for [CacheManager.getCache(name)] providing a `cm[name]` variant.
 *
 * @author Mikhael Sokolov
 * @since 5.2
 */
operator fun CacheManager.get(name: String): Cache? = getCache(name)