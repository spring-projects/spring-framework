/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvcview.mvcviewxsltbeandefs

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.view.xslt.XsltViewResolver

// tag::snippet[]
@Configuration
class WebConfiguration : WebMvcConfigurer {

	@Bean
	fun xsltViewResolver() = XsltViewResolver().apply {
		setPrefix("/WEB-INF/xsl/")
		setSuffix(".xslt")	
	}
}
// end::snippet[]
