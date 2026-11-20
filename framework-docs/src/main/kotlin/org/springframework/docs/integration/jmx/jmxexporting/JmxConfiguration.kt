/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jmx.jmxexporting

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jmx.export.MBeanExporter

// tag::snippet[]
@Configuration
class JmxConfiguration {

	@Bean
	fun exporter(testBean: JmxTestBean) = MBeanExporter().apply {
		setBeans(mapOf("bean:name=testBean1" to testBean))
	}

	@Bean
	fun testBean() = JmxTestBean().apply {
		name = "TEST"
		age = 100
	}
}
// end::snippet[]