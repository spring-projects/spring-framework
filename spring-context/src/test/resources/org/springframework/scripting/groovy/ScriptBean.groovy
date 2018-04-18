package org.springframework.scripting.groovy;

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.scripting.ContextScriptBean
import org.springframework.tests.sample.beans.TestBean

class GroovyScriptBean implements ContextScriptBean, ApplicationContextAware {

	private int age

	int getAge() {
		return this.age
	}

	void setAge(int age) {
		this.age = age
	}

	def String name

	def TestBean testBean;

	def ApplicationContext applicationContext
}
