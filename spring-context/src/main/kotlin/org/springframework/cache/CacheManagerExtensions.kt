/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache

/**
 * Extension for [CacheManager.getCache(name)] providing a `cm[name]` variant.
 *
 * @author Mikhael Sokolov
 * @since 6.0
 */
operator fun CacheManager.get(name: String): Cache? = getCache(name)