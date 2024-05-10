package org.springframework.circular.dependency.model;

public class InstanceB {

	private InstanceA instanceA;

	public InstanceB() {
		System.out.println("InstanceB()");
	}

	public InstanceB(InstanceA instanceA) {
		this.instanceA = instanceA;
	}

	public InstanceA getInstanceA() {
		return instanceA;
	}

	public void setInstanceA(InstanceA instanceA) {
		this.instanceA = instanceA;
	}
}
