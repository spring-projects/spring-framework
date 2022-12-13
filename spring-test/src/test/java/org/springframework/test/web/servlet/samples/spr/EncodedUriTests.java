/*
 * Copyright 2002-2022 the original author or authors.
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


import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UrlPathHelper;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Tests for SPR-11441 (MockMvc accepts an already encoded URI).
 *
 * @author Sebastien Deleuze
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration
public class EncodedUriTests {

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	@BeforeEach
	public void setup() {
		this.mockMvc = webAppContextSetup(this.wac).build();
	}

	@Test
	public void test() throws Exception {
		String id = "a/b";
		URI url = UriComponentsBuilder.fromUriString("/circuit").pathSegment(id).build().encode().toUri();
		ResultActions result = mockMvc.perform(get(url));
		result.andExpect(status().isOk()).andExpect(model().attribute("receivedId", is(id)));
	}


	@Configuration
	@EnableWebMvc
	static class WebConfig implements WebMvcConfigurer {

		@Bean
		public MyController myController() {
			return new MyController();
		}

		@Bean
		public HandlerMappingConfigurer myHandlerMappingConfigurer() {
			return new HandlerMappingConfigurer();
		}

		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			registry.jsp("", "");
		}
	}

	@Controller
	private static class MyController {

		@RequestMapping(value = "/circuit/{id}", method = RequestMethod.GET)
		public String getCircuit(@PathVariable String id, Model model) {
			model.addAttribute("receivedId", id);
			return "result";
		}
	}

	@Component
	private static class HandlerMappingConfigurer implements BeanPostProcessor, PriorityOrdered {

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof RequestMappingHandlerMapping requestMappingHandlerMapping) {
				// URL decode after request mapping, not before.
				UrlPathHelper pathHelper = new UrlPathHelper();
				pathHelper.setUrlDecode(false);
				requestMappingHandlerMapping.setUrlPathHelper(pathHelper);
			}
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			return bean;
		}

		@Override
		public int getOrder() {
			return PriorityOrdered.HIGHEST_PRECEDENCE;
		}
	}

}
