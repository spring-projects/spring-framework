package com.ysj.delegate;

public class B implements A{
	@Override
	public void delegateMethod() {
		System.out.println("B...");
	}
}
