/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PersonWithAlias(

	@get:AliasFor("name")
	val value: String = "",

	@get:AliasFor("value")
	val name: String = "",

	vararg val friends: PersonWithAlias = []

)
