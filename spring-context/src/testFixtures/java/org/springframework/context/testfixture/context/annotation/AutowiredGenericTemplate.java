/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.context.annotation;

import org.springframework.beans.factory.annotation.Autowired;

public class AutowiredGenericTemplate {

	@SuppressWarnings("unused")
	private GenericTemplate<Integer> genericTemplate;

	@Autowired
	public void setGenericTemplate(GenericTemplate<Integer> genericTemplate) {
		this.genericTemplate = genericTemplate;
	}

}
