/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans

class KotlinConfiguration {

	fun stringBean(): String {
		return "Hello"
	}

	suspend fun suspendingStringBean(): String {
		return "Hello"
	}
}
