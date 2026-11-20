/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
