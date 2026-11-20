/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jmx.jmxexporting

// tag::snippet[]
class JmxTestBean : IJmxTestBean {

	override lateinit var name: String
	override var age = 0

	override fun add(x: Int, y: Int): Int {
		return x + y
	}

	override fun dontExposeMe() {
		throw RuntimeException()
	}
}
// end::snippet[]