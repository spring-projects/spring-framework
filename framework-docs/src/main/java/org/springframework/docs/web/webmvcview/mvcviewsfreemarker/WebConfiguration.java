/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvcview.mvcviewsfreemarker;

import java.util.Map;

import freemarker.template.utility.XmlEscape;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

@Configuration
public class WebConfiguration {

	// tag::snippet[]
	@Bean
	public FreeMarkerConfigurer freeMarkerConfigurer() {
		FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
		configurer.setTemplateLoaderPath("/WEB-INF/freemarker");
		configurer.setFreemarkerVariables(Map.of("xml_escape", new XmlEscape()));
		return configurer;
	}
	// end::snippet[]
}
