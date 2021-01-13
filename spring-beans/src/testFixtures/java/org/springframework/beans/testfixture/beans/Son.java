package org.springframework.beans.testfixture.beans;

/**
 * @author jinxiong
 */
public class Son {

	private Father father;

	private Mother mother;

	public Father getFather() {
		return father;
	}

	public void setFather(Father father) {
		this.father = father;
	}

	public Mother getMother() {
		return mother;
	}

	public void setMother(Mother mother) {
		this.mother = mother;
	}
}
