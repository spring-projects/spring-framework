/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.ui

/**
 * Extension for [Model.addAttribute] providing Array like setter.
 *
 * ```kotlin
 * model["firstName"] = "Mario"
 * ```
 *
 * @author Mario Arias
 * @since 5.0
 */
operator fun Model.set(attributeName: String, attributeValue: Any?) {
	this.addAttribute(attributeName, attributeValue)
}
