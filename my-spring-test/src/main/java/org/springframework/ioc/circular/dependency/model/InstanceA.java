package org.springframework.ioc.circular.dependency.model;

public class InstanceA {

	private InstanceB instanceB;

	public InstanceA() {
		System.out.println("InstanceA()");
	}
	public InstanceA(InstanceB instanceB) {
		this.instanceB = instanceB;
	}

	public InstanceB getInstanceB() {
		return instanceB;
	}

	public void setInstanceB(InstanceB instanceB) {
		this.instanceB = instanceB;
	}
}
