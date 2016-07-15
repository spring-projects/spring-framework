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

package org.springframework.web.servlet.config;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.cors.CorsConfiguration;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses a
 * {@code cors} element in order to set the CORS configuration in the various
 * {AbstractHandlerMapping} beans created by {@link AnnotationDrivenBeanDefinitionParser},
 * {@link ResourcesBeanDefinitionParser} and {@link ViewControllerBeanDefinitionParser}.
 *
 * @author Sebastien Deleuze
 * @since 4.2
 */
public class CorsBeanDefinitionParser implements BeanDefinitionParser {

	private static final List<String> DEFAULT_ALLOWED_ORIGINS = Arrays.asList("*");

	private static final List<String> DEFAULT_ALLOWED_METHODS =
			Arrays.asList(HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.POST.name());

	private static final List<String> DEFAULT_ALLOWED_HEADERS = Arrays.asList("*");

	private static final boolean DEFAULT_ALLOW_CREDENTIALS = true;

	private static final long DEFAULT_MAX_AGE = 1600;


	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {

		Map<String, CorsConfiguration> corsConfigurations = new LinkedHashMap<>();
		List<Element> mappings = DomUtils.getChildElementsByTagName(element, "mapping");

		if (mappings.isEmpty()) {
			CorsConfiguration config = new CorsConfiguration();
			config.setAllowedOrigins(DEFAULT_ALLOWED_ORIGINS);
			config.setAllowedMethods(DEFAULT_ALLOWED_METHODS);
			config.setAllowedHeaders(DEFAULT_ALLOWED_HEADERS);
			config.setAllowCredentials(DEFAULT_ALLOW_CREDENTIALS);
			config.setMaxAge(DEFAULT_MAX_AGE);
			corsConfigurations.put("/**", config);
		}
		else {
			for (Element mapping : mappings) {
				CorsConfiguration config = new CorsConfiguration();
				if (mapping.hasAttribute("allowed-origins")) {
					String[] allowedOrigins = StringUtils.tokenizeToStringArray(mapping.getAttribute("allowed-origins"), ",");
					config.setAllowedOrigins(Arrays.asList(allowedOrigins));
				}
				else {
					config.setAllowedOrigins(DEFAULT_ALLOWED_ORIGINS);
				}
				if (mapping.hasAttribute("allowed-methods")) {
					String[] allowedMethods = StringUtils.tokenizeToStringArray(mapping.getAttribute("allowed-methods"), ",");
					config.setAllowedMethods(Arrays.asList(allowedMethods));
				}
				else {
					config.setAllowedMethods(DEFAULT_ALLOWED_METHODS);
				}
				if (mapping.hasAttribute("allowed-headers")) {
					String[] allowedHeaders = StringUtils.tokenizeToStringArray(mapping.getAttribute("allowed-headers"), ",");
					config.setAllowedHeaders(Arrays.asList(allowedHeaders));
				}
				else {
					config.setAllowedHeaders(DEFAULT_ALLOWED_HEADERS);
				}
				if (mapping.hasAttribute("exposed-headers")) {
					String[] exposedHeaders = StringUtils.tokenizeToStringArray(mapping.getAttribute("exposed-headers"), ",");
					config.setExposedHeaders(Arrays.asList(exposedHeaders));
				}
				if (mapping.hasAttribute("allow-credentials")) {
					config.setAllowCredentials(Boolean.parseBoolean(mapping.getAttribute("allow-credentials")));
				}
				else {
					config.setAllowCredentials(DEFAULT_ALLOW_CREDENTIALS);
				}
				if (mapping.hasAttribute("max-age")) {
					config.setMaxAge(Long.parseLong(mapping.getAttribute("max-age")));
				}
				else {
					config.setMaxAge(DEFAULT_MAX_AGE);
				}
				corsConfigurations.put(mapping.getAttribute("path"), config);
			}
		}

		MvcNamespaceUtils.registerCorsConfigurations(corsConfigurations, parserContext, parserContext.extractSource(element));
		return null;
	}

}
