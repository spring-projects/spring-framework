/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.context.annotation;

import static java.lang.String.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.context.annotation.ConfigurationClass;
import org.springframework.context.annotation.ConfigurationModel;


/**
 * Unit tests for {@link ConfigurationModel}.
 * 
 * @author Chris Beams
 */
public class ConfigurationModelTests {

	@Test
	public void testToString() {
		ConfigurationModel model = new ConfigurationModel();
		assertThat(model.toString(), equalTo(
				"ConfigurationModel containing @Configuration classes: []"));

		ConfigurationClass config1 = new ConfigurationClass();
		config1.setName("test.Config1");
		model.add(config1);

		assertThat(model.toString(), equalTo(format(
				"ConfigurationModel containing @Configuration classes: [%s]", config1)));
	}

}
