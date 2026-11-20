/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.xml;


import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.beans.testfixture.beans.factory.DummyFactory;

/**
 * @author Juergen Hoeller
 * @since 21.07.2003
 */
public class DummyReferencer {

	private TestBean testBean1;

	private TestBean testBean2;

	private DummyFactory dummyFactory;


	public DummyReferencer() {
	}

	public DummyReferencer(DummyFactory dummyFactory) {
		this.dummyFactory = dummyFactory;
	}

	public void setDummyFactory(DummyFactory dummyFactory) {
		this.dummyFactory = dummyFactory;
	}

	public DummyFactory getDummyFactory() {
		return dummyFactory;
	}

	public void setTestBean1(TestBean testBean1) {
		this.testBean1 = testBean1;
	}

	public TestBean getTestBean1() {
		return testBean1;
	}

	public void setTestBean2(TestBean testBean2) {
		this.testBean2 = testBean2;
	}

	public TestBean getTestBean2() {
		return testBean2;
	}

}
