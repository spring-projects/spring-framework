package com.ysj.tte;


public class MainTte {
	public static void main(String[] args)
	{
		staticFunction();
	}

	static MainTte book = new MainTte();

	static
	{
		System.out.println("书的静态代码块");
	}

	{
		System.out.println("书的普通代码块");
	}

	MainTte()
	{
		System.out.println("书的构造方法");
		System.out.println("price=" + price +",amount=" + amount);
	}

	public static void staticFunction(){
		System.out.println("书的静态方法");
	}

	int price = 110;
	static int amount = 112;
}
