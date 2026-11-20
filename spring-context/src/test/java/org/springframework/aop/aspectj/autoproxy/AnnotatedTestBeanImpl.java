/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj.autoproxy;

/**
 * @author Adrian Colyer
 * @since 2.0
 */
class AnnotatedTestBeanImpl implements AnnotatedTestBean {

	@Override
	@TestAnnotation("this value")
	public String doThis() {
		return "doThis";
	}

	@Override
	@TestAnnotation("that value")
	public String doThat() {
		return "doThat";
	}

	@Override
	@TestAnnotation("array value")
	public String[] doArray() {
		return new String[] {"doThis", "doThat"};
	}

	// not annotated
	@Override
	public String doTheOther() {
		return "doTheOther";
	}

}
