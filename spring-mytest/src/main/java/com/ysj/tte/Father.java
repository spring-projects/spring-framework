package com.ysj.tte;

public class Father extends Grandpa{

	public void name(){
		System.out.println("父类方法");
	}

	@Override
	public void big() {
		System.out.println("big-父类");
	}
}
