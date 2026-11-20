/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvccontroller.mvcanncontroller

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

// tag::snippet[]
@Configuration
@ComponentScan("org.example.web")
class WebConfiguration {

	// ...
}
// end::snippet[]
