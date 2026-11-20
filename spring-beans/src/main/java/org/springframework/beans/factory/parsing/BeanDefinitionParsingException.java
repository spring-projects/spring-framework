/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.parsing;

import org.springframework.beans.factory.BeanDefinitionStoreException;

/**
 * Exception thrown when a bean definition reader encounters an error
 * during the parsing process.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 2.0
 */
@SuppressWarnings("serial")
public class BeanDefinitionParsingException extends BeanDefinitionStoreException {

	/**
	 * Create a new BeanDefinitionParsingException.
	 * @param problem the configuration problem that was detected during the parsing process
	 */
	public BeanDefinitionParsingException(Problem problem) {
		super(problem.getResourceDescription(), problem.toString(), problem.getRootCause());
	}

}
