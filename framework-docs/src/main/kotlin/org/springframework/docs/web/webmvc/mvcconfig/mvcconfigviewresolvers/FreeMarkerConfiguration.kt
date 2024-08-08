package org.springframework.docs.web.webmvc.mvcconfig.mvcconfigviewresolvers

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer
import org.springframework.web.servlet.view.json.MappingJackson2JsonView

// tag::snippet[]
@Configuration
class FreeMarkerConfiguration : WebMvcConfigurer {

	override fun configureViewResolvers(registry: ViewResolverRegistry) {
		registry.enableContentNegotiation(MappingJackson2JsonView())
		registry.freeMarker().cache(false)
	}

	@Bean
	fun freeMarkerConfigurer() = FreeMarkerConfigurer().apply {
		setTemplateLoaderPath("/freemarker")
	}
}
// end::snippet[]