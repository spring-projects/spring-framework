/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcconfig.mvcconfigvalidation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

// tag::snippet[]
@Controller
public class MyController {

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.addValidators(new FooValidator());
	}
}
// end::snippet[]
