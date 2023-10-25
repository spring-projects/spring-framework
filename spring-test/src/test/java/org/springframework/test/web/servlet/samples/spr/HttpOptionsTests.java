/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.servlet.samples.spr;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Tests for SPR-10093 (support for OPTIONS requests).
 *
 * @author Arnaud Cogoluègnes
 */
@SpringJUnitWebConfig
public class HttpOptionsTests {

	private final WebApplicationContext wac;

	private final MockMvc mockMvc;

	HttpOptionsTests(WebApplicationContext wac) {
		this.wac = wac;
		this.mockMvc = webAppContextSetup(this.wac).dispatchOptions(true).build();
	}


	@Test
	void test() throws Exception {
		MyController controller = this.wac.getBean(MyController.class);
		int initialCount = controller.counter.get();
		this.mockMvc.perform(options("/myUrl")).andExpect(status().isOk());

		assertThat(controller.counter.get()).isEqualTo((initialCount + 1));
	}


	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	static class WebConfig implements WebMvcConfigurer {

		@Bean
		MyController myController() {
			return new MyController();
		}
	}

	@Controller
	static class MyController {

		private final AtomicInteger counter = new AtomicInteger();


		@RequestMapping(value = "/myUrl", method = RequestMethod.OPTIONS)
		@ResponseBody
		void handle() {
			counter.incrementAndGet();
		}
	}

}
