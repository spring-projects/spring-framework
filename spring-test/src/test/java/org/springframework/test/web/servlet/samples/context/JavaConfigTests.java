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

package org.springframework.test.web.servlet.samples.context;

import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.Person;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.samples.context.JavaConfigTests.RootConfig;
import org.springframework.test.web.servlet.samples.context.JavaConfigTests.WebConfig;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests with Java configuration.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author Michał Rowicki
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration("classpath:META-INF/web-resources")
@ContextHierarchy({
	@ContextConfiguration(classes = RootConfig.class),
	@ContextConfiguration(classes = WebConfig.class)
})
@DisabledInAotMode // @ContextHierarchy is not supported in AOT.
public class JavaConfigTests {

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private PersonDao personDao;

	@Autowired
	private PersonController personController;

	private MockMvc mockMvc;


	@BeforeEach
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
		verifyRootWacSupport();
		given(this.personDao.getPerson(5L)).willReturn(new Person("Joe"));
	}

	@Test
	public void person() throws Exception {
		this.mockMvc.perform(get("/person/5").accept(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpectAll(
				status().isOk(),
				request().asyncNotStarted(),
				content().string("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}"),
				jsonPath("$.name").value("Joe")
			);
	}

	@Test
	public void andExpectAllWithOneFailure() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> this.mockMvc.perform(get("/person/5").accept(MediaType.APPLICATION_JSON))
					.andExpectAll(
						status().isBadGateway(),
						request().asyncNotStarted(),
						jsonPath("$.name").value("Joe")))
			.withMessage("Status expected:<502> but was:<200>")
			.satisfies(error -> assertThat(error).hasNoSuppressedExceptions());
	}

	@Test
	public void andExpectAllWithMultipleFailures() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
			this.mockMvc.perform(get("/person/5").accept(MediaType.APPLICATION_JSON))
				.andExpectAll(
					status().isBadGateway(),
					request().asyncNotStarted(),
					jsonPath("$.name").value("Joe"),
					jsonPath("$.name").value("Jane")
				))
			.withMessage("Multiple Exceptions (2):\nStatus expected:<502> but was:<200>\nJSON path \"$.name\" expected:<Jane> but was:<Joe>")
			.satisfies(error -> assertThat(error.getSuppressed()).hasSize(2));
	}

	/**
	 * Verify that the breaking change introduced in <a
	 * href="https://jira.spring.io/browse/SPR-12553">SPR-12553</a> has been reverted.
	 *
	 * <p>This code has been copied from
	 * {@link org.springframework.test.context.hierarchies.web.ControllerIntegrationTests}.
	 *
	 * @see org.springframework.test.context.hierarchies.web.ControllerIntegrationTests#verifyRootWacSupport()
	 */
	private void verifyRootWacSupport() {
		assertThat(personDao).isNotNull();
		assertThat(personController).isNotNull();

		ApplicationContext parent = wac.getParent();
		assertThat(parent).isNotNull();
		assertThat(parent).isInstanceOf(WebApplicationContext.class);
		WebApplicationContext root = (WebApplicationContext) parent;

		ServletContext childServletContext = wac.getServletContext();
		assertThat(childServletContext).isNotNull();
		ServletContext rootServletContext = root.getServletContext();
		assertThat(rootServletContext).isNotNull();
		assertThat(rootServletContext).isSameAs(childServletContext);

		assertThat(rootServletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).isSameAs(root);
		assertThat(childServletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).isSameAs(root);
	}


	@Configuration
	static class RootConfig {

		@Bean
		public PersonDao personDao() {
			return mock();
		}
	}

	@Configuration
	@EnableWebMvc
	static class WebConfig implements WebMvcConfigurer {

		@Autowired
		private RootConfig rootConfig;

		@Bean
		public PersonController personController() {
			return new PersonController(this.rootConfig.personDao());
		}

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/resources/**").addResourceLocations("/resources/");
		}

		@Override
		public void addViewControllers(ViewControllerRegistry registry) {
			registry.addViewController("/").setViewName("home");
		}

		@Override
		public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
			configurer.enable();
		}
	}

}
