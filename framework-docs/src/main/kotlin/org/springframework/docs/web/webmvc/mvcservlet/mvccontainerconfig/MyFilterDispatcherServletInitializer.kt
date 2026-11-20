/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcservlet.mvccontainerconfig

import jakarta.servlet.Filter
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.filter.CharacterEncodingFilter
import org.springframework.web.filter.HiddenHttpMethodFilter
import org.springframework.web.servlet.support.AbstractDispatcherServletInitializer

// tag::snippet[]
class MyFilterDispatcherServletInitializer : AbstractDispatcherServletInitializer() {

	override fun getServletFilters(): Array<Filter> {
		return arrayOf(HiddenHttpMethodFilter(), CharacterEncodingFilter())
	}

	// @fold:on
	override fun createServletApplicationContext(): WebApplicationContext {
		/**/TODO("Not yet implemented")
	}

	override fun getServletMappings(): Array<String> {
		/**/TODO("Not yet implemented")
	}

	override fun createRootApplicationContext(): WebApplicationContext? {
		/**/TODO("Not yet implemented")
	}
	// @fold:off
}
// end::snippet[]
