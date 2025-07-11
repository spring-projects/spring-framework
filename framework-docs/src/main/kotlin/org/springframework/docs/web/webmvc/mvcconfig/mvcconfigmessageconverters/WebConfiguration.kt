@file:Suppress("DEPRECATION")

package org.springframework.docs.web.webmvc.mvcconfig.mvcconfigmessageconverters

import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.dataformat.xml.XmlMapper
import java.text.SimpleDateFormat

// tag::snippet[]
@Configuration
class WebConfiguration : WebMvcConfigurer {

	override fun configureMessageConverters(builder: HttpMessageConverters.ServerBuilder) {
		val jsonMapper = JsonMapper.builder()
			.findAndAddModules()
			.enable(SerializationFeature.INDENT_OUTPUT)
			.defaultDateFormat(SimpleDateFormat("yyyy-MM-dd"))
			.build()
		val xmlMapper = XmlMapper.builder()
			.findAndAddModules()
			.defaultUseWrapper(false)
			.build()
		builder.jsonMessageConverter(JacksonJsonHttpMessageConverter(jsonMapper))
			.xmlMessageConverter(JacksonXmlHttpMessageConverter(xmlMapper))
	}
}
// end::snippet[]