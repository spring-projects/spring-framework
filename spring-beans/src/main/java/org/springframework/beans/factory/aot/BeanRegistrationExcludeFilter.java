/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.aot;

import org.springframework.beans.factory.support.RegisteredBean;

/**
 * Filter that can be used to exclude AOT processing of a
 * {@link RegisteredBean}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 */
@FunctionalInterface
public interface BeanRegistrationExcludeFilter {

	/**
	 * Return if the registered bean should be excluded from AOT processing and
	 * registration.
	 * @param registeredBean the registered bean
	 * @return if the registered bean should be excluded
	 */
	boolean isExcludedFromAotProcessing(RegisteredBean registeredBean);

}
