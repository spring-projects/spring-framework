/*
 * Copyright 2002-2025 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.Person;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests with XML configuration.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration("src/test/resources/META-INF/web-resources")
@ContextHierarchy({
	@ContextConfiguration("root-context.xml"),
	@ContextConfiguration("servlet-context.xml")
})
@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
public class XmlConfigTests {

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private PersonDao personDao;

	private MockMvc mockMvc;


	@BeforeEach
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
		given(this.personDao.getPerson(5L)).willReturn(new Person("Joe"));
	}

	@Test
	public void person() throws Exception {
		this.mockMvc.perform(get("/person/5").accept(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().string("{\"name\":\"Joe\",\"someBoolean\":false,\"someDouble\":0.0}"));
	}

}
