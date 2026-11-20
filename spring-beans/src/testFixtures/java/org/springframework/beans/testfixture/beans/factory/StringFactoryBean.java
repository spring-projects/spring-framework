/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory;

import org.springframework.beans.factory.FactoryBean;

public class StringFactoryBean implements FactoryBean<String> {

	@Override
	public String getObject() {
		return "";
	}

	@Override
	public Class<String> getObjectType() {
		return String.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}


