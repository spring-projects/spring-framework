package com.ysj.delegate;

public class DelegateMain {
	public static void main(String[] args) {
		A a = new B();
		a.delegateMethod();
	}
}
