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

package org.springframework.test.context.junit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.*;

/**
 * JUnit 4 based integration test which verifies support of {@link @ContextConfiguration}
 * is optional.
 *
 * @author Phillip Webb
 * @since 4.3
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class OptionalContextConfigurationSpringRunnerTests {

	@Autowired
	private Config config;

	@Test
	public void testContextConfigurationIsOptional() throws Exception {
		assertNotNull(this.config);
	}

	@Configuration
	static class Config {
	}

}
