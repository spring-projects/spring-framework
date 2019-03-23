/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.validation.beanvalidation;

import javax.validation.ValidationException;

import org.apache.commons.logging.LogFactory;

/**
 * {@link LocalValidatorFactoryBean} subclass that simply turns
 * {@link org.springframework.validation.Validator} calls into no-ops
 * in case of no Bean Validation provider being available.
 *
 * <p>This is the actual class used by Spring's MVC configuration namespace,
 * in case of the {@code javax.validation} API being present but no explicit
 * Validator having been configured.
 *
 * @author Juergen Hoeller
 * @since 4.0.1
 */
public class OptionalValidatorFactoryBean extends LocalValidatorFactoryBean {

	@Override
	public void afterPropertiesSet() {
		try {
			super.afterPropertiesSet();
		}
		catch (ValidationException ex) {
			LogFactory.getLog(getClass()).debug("Failed to set up a Bean Validation provider", ex);
		}
	}

}
