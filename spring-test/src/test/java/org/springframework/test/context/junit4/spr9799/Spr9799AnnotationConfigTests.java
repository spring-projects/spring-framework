/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.context.junit4.spr9799;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Integration tests used to assess claims raised in
 * <a href="https://jira.spring.io/browse/SPR-9799" target="_blank">SPR-9799</a>.
 *
 * @author Sam Brannen
 * @since 3.2
 * @see Spr9799XmlConfigTests
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
// NOTE: if we omit the @WebAppConfiguration declaration, the ApplicationContext will fail
// to load since @EnableWebMvc requires that the context be a WebApplicationContext.
@WebAppConfiguration
public class Spr9799AnnotationConfigTests {

	@Configuration
	@EnableWebMvc
	static class Config {
		/* intentionally no beans defined */
	}


	@Test
	public void applicationContextLoads() {
		// no-op
	}

}
