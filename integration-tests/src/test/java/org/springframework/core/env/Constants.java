/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.core.env;

/**
 * Constants used both locally and in scan* sub-packages
 */
public class Constants {

	public static final String XML_PATH = "org/springframework/core/env/EnvironmentSystemIntegrationTests-context.xml";

	public static final String ENVIRONMENT_AWARE_BEAN_NAME = "envAwareBean";

	public static final String PROD_BEAN_NAME = "prodBean";
	public static final String DEV_BEAN_NAME = "devBean";
	public static final String DERIVED_DEV_BEAN_NAME = "derivedDevBean";
	public static final String TRANSITIVE_BEAN_NAME = "transitiveBean";

	public static final String PROD_ENV_NAME = "prod";
	public static final String DEV_ENV_NAME = "dev";
	public static final String DERIVED_DEV_ENV_NAME = "derivedDev";
}
