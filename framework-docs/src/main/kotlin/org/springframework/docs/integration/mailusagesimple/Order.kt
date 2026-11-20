/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.mailusagesimple

data class Order(
	val customer: Customer,
	val orderNumber: String
)