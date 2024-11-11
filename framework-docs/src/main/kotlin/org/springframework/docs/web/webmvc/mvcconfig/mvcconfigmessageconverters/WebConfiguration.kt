package org.springframework.docs.web.webmvc.mvcconfig.mvcconfigmessageconverters

import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.text.SimpleDateFormat

// tag::snippet[]
@Configuration
class WebConfiguration : WebMvcConfigurer {

	override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
		val builder = Jackson2ObjectMapperBuilder()
			.indentOutput(true)
			.dateFormat(SimpleDateFormat("yyyy-MM-dd"))
			.modulesToInstall(ParameterNamesModule())
		converters.add(MappingJackson2HttpMessageConverter(builder.build()))
		converters.add(MappingJackson2XmlHttpMessageConverter(builder.createXmlMapper(true).build()))
	}
}
// end::snippet[]