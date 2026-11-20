/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

@file:Suppress("DEPRECATION")

package org.springframework.docs.testing.mockmvc.assertj.mockmvctestersetup.converter

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.docs.testing.mockmvc.assertj.mockmvctestersetup.ApplicationWebConfiguration
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.web.context.WebApplicationContext

// tag::snippet[]
@SpringJUnitWebConfig(ApplicationWebConfiguration::class)
class AccountControllerIntegrationTests(@Autowired wac: WebApplicationContext) {

	private val mockMvc = MockMvcTester.from(wac).withHttpMessageConverters(
		listOf(wac.getBean(AbstractJacksonHttpMessageConverter::class.java)))

	// ...

}
// end::snippet[]

