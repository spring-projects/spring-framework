/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcconfig.mvcconfiginterceptors

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.handler.UserRoleAuthorizationInterceptor
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor

// tag::snippet[]
@Configuration
class WebConfiguration : WebMvcConfigurer {

	override fun addInterceptors(registry: InterceptorRegistry) {
		registry.addInterceptor(LocaleChangeInterceptor())
		registry.addInterceptor(UserRoleAuthorizationInterceptor()).addPathPatterns("/**").excludePathPatterns("/admin/**")
	}
}
// end::snippet[]