package com.lxcecho.aop.example;

/**
 * 基本实现类
 *
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/10
 */
public class CalculatorImpl implements Calculator {

	@Override
	public int add(int i, int j) {

		int result = i + j;

		System.out.println("方法内部 result = " + result);

		return result;
	}

	@Override
	public int sub(int i, int j) {

		int result = i - j;

		System.out.println("方法内部 result = " + result);

		return result;
	}

	@Override
	public int mul(int i, int j) {

		int result = i * j;

		System.out.println("方法内部 result = " + result);

		return result;
	}

	@Override
	public int div(int i, int j) {

		int result = i / j;

		System.out.println("方法内部 result = " + result);

		return result;
	}
}
