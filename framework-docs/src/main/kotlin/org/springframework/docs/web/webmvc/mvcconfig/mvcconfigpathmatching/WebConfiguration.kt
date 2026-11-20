/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcconfig.mvcconfigpathmatching

import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.method.HandlerTypePredicate
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.util.pattern.PathPatternParser

// tag::snippet[]
@Configuration
class WebConfiguration : WebMvcConfigurer {

	override fun configurePathMatch(configurer: PathMatchConfigurer) {
		configurer.addPathPrefix("/api", HandlerTypePredicate.forAnnotation(RestController::class.java))
	}

	fun patternParser(): PathPatternParser {
		val pathPatternParser = PathPatternParser()
		//...
		return pathPatternParser
	}
}
// end::snippet[]