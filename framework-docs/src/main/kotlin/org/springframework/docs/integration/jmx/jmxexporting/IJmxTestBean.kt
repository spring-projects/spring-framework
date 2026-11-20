/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jmx.jmxexporting

interface IJmxTestBean {

	var name: String
	var age: Int
	fun add(x: Int, y: Int): Int
	fun dontExposeMe()
}