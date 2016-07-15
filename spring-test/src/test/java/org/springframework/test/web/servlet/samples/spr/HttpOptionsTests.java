/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.test.web.servlet.samples.spr;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

/**
 * Tests for SPR-10093 (support for OPTIONS requests).
 *
 * @author Arnaud Cogoluègnes
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration
public class HttpOptionsTests {

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;


	@Before
	public void setup() {
		this.mockMvc = webAppContextSetup(this.wac).dispatchOptions(true).build();
	}

	@Test
	public void test() throws Exception {
		MyController controller = this.wac.getBean(MyController.class);
		int initialCount = controller.counter.get();
		this.mockMvc.perform(options("/myUrl")).andExpect(status().isOk());

		assertEquals(initialCount + 1, controller.counter.get());
	}


	@Configuration
	@EnableWebMvc
	static class WebConfig implements WebMvcConfigurer {

		@Bean
		public MyController myController() {
			return new MyController();
		}
	}

	@Controller
	private static class MyController {

		private AtomicInteger counter = new AtomicInteger(0);


		@RequestMapping(value = "/myUrl", method = RequestMethod.OPTIONS)
		@ResponseBody
		public void handle() {
			counter.incrementAndGet();
		}
	}

}
