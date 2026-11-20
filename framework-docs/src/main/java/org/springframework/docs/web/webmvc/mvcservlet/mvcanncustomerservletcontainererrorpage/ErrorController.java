/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcservlet.mvcanncustomerservletcontainererrorpage;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// tag::snippet[]
@RestController
public class ErrorController {

	@RequestMapping(path = "/error")
	public Map<String, Object> handle(HttpServletRequest request) {
		Map<String, Object> map = new HashMap<>();
		map.put("status", request.getAttribute("jakarta.servlet.error.status_code"));
		map.put("reason", request.getAttribute("jakarta.servlet.error.message"));
		return map;
	}
}
// end::snippet[]
