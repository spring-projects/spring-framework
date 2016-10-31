/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.result.method.annotation;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.config.EnableWebReactive;

import static org.junit.Assert.assertEquals;


/**
 * Data binding and type conversion related integration tests for
 * {@code @Controller}-annotated classes.
 *
 * @author Rossen Stoyanchev
 */
public class RequestMappingDataBindingIntegrationTests extends AbstractRequestMappingIntegrationTests {

	@Override
	protected ApplicationContext initApplicationContext() {
		AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();
		wac.register(WebConfig.class);
		wac.refresh();
		return wac;
	}


	@Test
	public void handleDateParam() throws Exception {
		assertEquals("Processed date!",
				performPost("/date-param?date=2016-10-31&date-pattern=YYYY-mm-dd",
						new HttpHeaders(), null, String.class).getBody());
	}


	@Configuration
	@EnableWebReactive
	@ComponentScan(resourcePattern = "**/RequestMappingDataBindingIntegrationTests*.class")
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class WebConfig {
	}

	@Controller
	@SuppressWarnings("unused")
	private static class TestController {

		@InitBinder
		public void initBinder(WebDataBinder dataBinder, @RequestParam("date-pattern") String pattern) {
			CustomDateEditor dateEditor = new CustomDateEditor(new SimpleDateFormat(pattern), false);
			dataBinder.registerCustomEditor(Date.class, dateEditor);
		}

		@PostMapping("/date-param")
		@ResponseBody
		public String handleDateParam(@RequestParam Date date) {
			return "Processed date!";
		}
	}

}
