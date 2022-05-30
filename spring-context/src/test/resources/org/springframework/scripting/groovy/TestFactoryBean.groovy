package org.springframework.scripting.groovy;

import org.springframework.beans.factory.FactoryBean

class TestFactoryBean implements FactoryBean {

	@Override
	public boolean isSingleton() {
		true
	}

	@Override
	public Class getObjectType() {
		String.class
	}

	@Override
	public Object getObject() {
		"test"
	}
}
