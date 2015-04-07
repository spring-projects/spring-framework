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

package org.springframework.test.web.servlet.samples.standalone;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.Person;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;
import org.springframework.web.servlet.view.xml.MarshallingView;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

/**
 * Tests with view resolution.
 *
 * @author Rossen Stoyanchev
 */
public class ViewResolutionTests {

	@Test
	public void testJspOnly() throws Exception {

		InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
		viewResolver.setPrefix("/WEB-INF/");
		viewResolver.setSuffix(".jsp");

		standaloneSetup(new PersonController()).setViewResolvers(viewResolver).build()
			.perform(get("/person/Corea"))
				.andExpect(status().isOk())
				.andExpect(model().size(1))
				.andExpect(model().attributeExists("person"))
				.andExpect(forwardedUrl("/WEB-INF/person/show.jsp"));
	}

	@Test
	public void testJsonOnly() throws Exception {

		standaloneSetup(new PersonController()).setSingleView(new MappingJackson2JsonView()).build()
			.perform(get("/person/Corea"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.person.name").value("Corea"));
	}

	@Test
	public void testXmlOnly() throws Exception {

		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(Person.class);

		standaloneSetup(new PersonController()).setSingleView(new MarshallingView(marshaller)).build()
			.perform(get("/person/Corea"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_XML))
				.andExpect(xpath("/person/name/text()").string(equalTo("Corea")));
	}

	@Test
	public void testContentNegotiation() throws Exception {

		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(Person.class);

		List<View> viewList = new ArrayList<View>();
		viewList.add(new MappingJackson2JsonView());
		viewList.add(new MarshallingView(marshaller));

		ContentNegotiationManager manager = new ContentNegotiationManager(
				new HeaderContentNegotiationStrategy(), new FixedContentNegotiationStrategy(MediaType.TEXT_HTML));

		ContentNegotiatingViewResolver cnViewResolver = new ContentNegotiatingViewResolver();
		cnViewResolver.setDefaultViews(viewList);
		cnViewResolver.setContentNegotiationManager(manager);
		cnViewResolver.afterPropertiesSet();

		MockMvc mockMvc =
			standaloneSetup(new PersonController())
				.setViewResolvers(cnViewResolver, new InternalResourceViewResolver())
				.build();

		mockMvc.perform(get("/person/Corea"))
			.andExpect(status().isOk())
			.andExpect(model().size(1))
			.andExpect(model().attributeExists("person"))
			.andExpect(forwardedUrl("person/show"));

		mockMvc.perform(get("/person/Corea").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.person.name").value("Corea"));

		mockMvc.perform(get("/person/Corea").accept(MediaType.APPLICATION_XML))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_XML))
			.andExpect(xpath("/person/name/text()").string(equalTo("Corea")));
	}

	@Test
	public void defaultViewResolver() throws Exception {

		standaloneSetup(new PersonController()).build()
			.perform(get("/person/Corea"))
				.andExpect(model().attribute("person", hasProperty("name", equalTo("Corea"))))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("person/show"));  // InternalResourceViewResolver
	}


	@Controller
	private static class PersonController {

		@RequestMapping(value="/person/{name}", method=RequestMethod.GET)
		public String show(@PathVariable String name, Model model) {
			Person person = new Person(name);
			model.addAttribute(person);
			return "person/show";
		}
	}

}

