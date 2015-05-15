/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.cors.CorsConfiguration;

/**
 * Assist with the registration of {@link CorsConfiguration} mapped to one or more path patterns.
 * @author Sebastien Deleuze
 * 
 * @since 4.2
 * @see CrossOriginRegistration
 */
public class CrossOriginConfigurer {
	
	private final List<CrossOriginRegistration> registrations = new ArrayList<CrossOriginRegistration>();

	/**
	 * Enable cross origin requests on the specified path patterns. If no path pattern is specified,
	 * cross-origin request handling is mapped on "/**" .
	 * 
	 * <p>By default, all origins, all headers and credentials are allowed. Max age is set to 30 minutes.</p>
	 */
	public CrossOriginRegistration enableCrossOrigin(String... pathPatterns) {
		CrossOriginRegistration registration = new CrossOriginRegistration(pathPatterns);
		this.registrations.add(registration);
		return registration;
	}
	
	protected Map<String, CorsConfiguration> getCorsConfigurations() {
		Map<String, CorsConfiguration> configs = new HashMap<String, CorsConfiguration>();
		for (CrossOriginRegistration registration : this.registrations) {
			for (String pathPattern : registration.getPathPatterns()) {
				configs.put(pathPattern, registration.getCorsConfiguration());
			}
		}
		return configs;
	}

}
