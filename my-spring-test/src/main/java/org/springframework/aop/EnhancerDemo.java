package org.springframework.aop;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * @author sushuaiqiang
 * @date 2024/10/24 - 14:27
 */
public class EnhancerDemo {
	public static void main(String[] args) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(EnhancerDemo.class);
		enhancer.setCallback(new MethodInterceptorImpl());

		EnhancerDemo enhancerDemo = (EnhancerDemo) enhancer.create();
		enhancerDemo.test();
		System.out.println(enhancerDemo);
	}

	public void test() {
		System.out.println("EnhancerDemo test()");
	}

	private static class MethodInterceptorImpl implements MethodInterceptor {
		@Override
		public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
			System.err.println("Before invoke:" + method);
			Object result = methodProxy.invokeSuper(object, args);
			System.err.println("After invoke:" + method);
			return result;
		}
	}
}
