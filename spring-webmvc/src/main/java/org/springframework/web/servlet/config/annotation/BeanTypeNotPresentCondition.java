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

package org.springframework.web.servlet.config.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ObjectUtils;

/**
 * A simple configuration condition that checks for the absence of any beans
 * of a given type.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
class BeanTypeNotPresentCondition implements ConfigurationCondition {

	private static final Log logger =
			LogFactory.getLog("org.springframework.web.servlet.config.annotation.ViewResolution");

	private final Class<?> beanType;


	BeanTypeNotPresentCondition(Class<?> beanType) {
		this.beanType = beanType;
	}


	@Override
	public ConfigurationPhase getConfigurationPhase() {
		return ConfigurationPhase.PARSE_CONFIGURATION;
	}

	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ListableBeanFactory factory = context.getBeanFactory();
		String[] names = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(factory, this.beanType, false, false);
		if (ObjectUtils.isEmpty(names)) {
			logger.debug("No bean of type [" + this.beanType + "]. Conditional configuration applies.");
			return true;
		}
		else {
			logger.debug("Found bean of type [" + this.beanType + "]. Conditional configuration does not apply.");
			return false;
		}
	}

}
