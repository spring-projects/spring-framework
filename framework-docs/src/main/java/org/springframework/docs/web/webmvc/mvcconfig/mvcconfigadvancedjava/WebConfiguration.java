/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcconfig.mvcconfigadvancedjava;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

// tag::snippet[]
@Configuration
public class WebConfiguration extends DelegatingWebMvcConfiguration {

	// ...
}
// end::snippet[]