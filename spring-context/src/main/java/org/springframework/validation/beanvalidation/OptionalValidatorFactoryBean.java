/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.validation.beanvalidation;

import jakarta.validation.ValidationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * {@link LocalValidatorFactoryBean} subclass that simply turns
 * {@link org.springframework.validation.Validator} calls into no-ops
 * in case of no Bean Validation provider being available.
 *
 * <p>This is the actual class used by Spring's MVC configuration namespace,
 * in case of the {@code jakarta.validation} API being present but no explicit
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
			Log logger = LogFactory.getLog(getClass());
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to set up a Bean Validation provider", ex);
			}
			else if (logger.isInfoEnabled()) {
				logger.info("Failed to set up a Bean Validation provider: " + ex);
			}
		}
	}

}
