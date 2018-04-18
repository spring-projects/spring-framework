/*
 * Copyright 2002-2010 the original author or authors.
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

/**
 * Mirrors the structure of beans and environment-specific config files
 * in EnvironmentIntegrationTests-context.xml
 */
package org.springframework.core.env.scan1;

import static org.springframework.core.env.EnvironmentSystemIntegrationTests.Constants.DEV_ENV_NAME;
import static org.springframework.core.env.EnvironmentSystemIntegrationTests.Constants.PROD_ENV_NAME;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Configuration
@Import({DevConfig.class, ProdConfig.class})
class Config {
}

@Profile(DEV_ENV_NAME)
@Configuration
class DevConfig {
	@Bean
	public Object devBean() {
		return new Object();
	}
}

@Profile(PROD_ENV_NAME)
@Configuration
class ProdConfig {
	@Bean
	public Object prodBean() {
		return new Object();
	}
}
