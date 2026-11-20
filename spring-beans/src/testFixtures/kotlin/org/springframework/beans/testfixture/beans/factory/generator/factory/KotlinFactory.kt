/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.generator.factory

class KotlinFactory {

	companion object {

		@JvmStatic
		fun create() = "test"

		@JvmStatic
		suspend fun suspendingCreate() = "test"
	}
}
