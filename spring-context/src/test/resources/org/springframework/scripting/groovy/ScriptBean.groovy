package org.springframework.scripting.groovy;

import org.springframework.beans.testfixture.beans.TestBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.scripting.ContextScriptBean

class GroovyScriptBean implements ContextScriptBean, ApplicationContextAware {

	private int age

	@Override
	int getAge() {
		return this.age
	}

	@Override
	void setAge(int age) {
		this.age = age
	}

	def String name

	def TestBean testBean;

	def ApplicationContext applicationContext
}
