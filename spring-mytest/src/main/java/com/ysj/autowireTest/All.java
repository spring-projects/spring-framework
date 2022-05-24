package com.ysj.autowireTest;

public class All implements D{
	private A a;
	private C c;
	@Override
	public void setA(A a) {
		this.a = a;
	}
	public A getA() {
		return a;
	}

	public C getC() {
		return c;
	}

	public void setC(C c) {
		this.c = c;
	}
}
