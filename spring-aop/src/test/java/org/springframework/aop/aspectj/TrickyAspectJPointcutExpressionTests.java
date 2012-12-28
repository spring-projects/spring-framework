package org.springframework.aop.aspectj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.core.OverridingClassLoader;

/**
 * @author Dave Syer
 */
public class TrickyAspectJPointcutExpressionTests {

	@Test
	public void testManualProxyJavaWithUnconditionalPointcut() throws Exception {
		TestService target = new TestServiceImpl();
		LogUserAdvice logAdvice = new LogUserAdvice();
		testAdvice(new DefaultPointcutAdvisor(logAdvice), logAdvice, target, "TestServiceImpl");
	}

	@Test
	public void testManualProxyJavaWithStaticPointcut() throws Exception {
		TestService target = new TestServiceImpl();
		LogUserAdvice logAdvice = new LogUserAdvice();
		AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
		pointcut.setExpression(String.format("execution(* %s.TestService.*(..))", getClass().getName()));
		testAdvice(new DefaultPointcutAdvisor(pointcut, logAdvice), logAdvice, target, "TestServiceImpl");
	}

	@Test
	public void testManualProxyJavaWithDynamicPointcut() throws Exception {
		TestService target = new TestServiceImpl();
		LogUserAdvice logAdvice = new LogUserAdvice();
		AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
		pointcut.setExpression(String.format("@within(%s.Log)", getClass().getName()));
		testAdvice(new DefaultPointcutAdvisor(pointcut, logAdvice), logAdvice, target, "TestServiceImpl");
	}

	@Test
	public void testManualProxyJavaWithDynamicPointcutAndProxyTargetClass() throws Exception {
		TestService target = new TestServiceImpl();
		LogUserAdvice logAdvice = new LogUserAdvice();
		AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
		pointcut.setExpression(String.format("@within(%s.Log)", getClass().getName()));
		testAdvice(new DefaultPointcutAdvisor(pointcut, logAdvice), logAdvice, target, "TestServiceImpl", true);
	}

	@Test
	public void testManualProxyJavaWithStaticPointcutAndTwoClassLoaders() throws Exception {

		LogUserAdvice logAdvice = new LogUserAdvice();
		AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
		pointcut.setExpression(String.format("execution(* %s.TestService.*(..))", getClass().getName()));

		// Test with default class loader first...
		testAdvice(new DefaultPointcutAdvisor(pointcut, logAdvice), logAdvice, new TestServiceImpl(), "TestServiceImpl");

		// Then try again with a different class loader on the target...
		SimpleThrowawayClassLoader loader = new SimpleThrowawayClassLoader(new TestServiceImpl().getClass().getClassLoader());
		// Make sure the interface is loaded from the  parent class loader
		loader.excludeClass(TestService.class.getName());
		loader.excludeClass(TestException.class.getName());
		TestService other = (TestService) loader.loadClass(TestServiceImpl.class.getName()).newInstance();
		testAdvice(new DefaultPointcutAdvisor(pointcut, logAdvice), logAdvice, other, "TestServiceImpl");

	}

	private void testAdvice(Advisor advisor, LogUserAdvice logAdvice, TestService target, String message)
			throws Exception {
		testAdvice(advisor, logAdvice, target, message, false);
	}

	private void testAdvice(Advisor advisor, LogUserAdvice logAdvice, TestService target, String message,
			boolean proxyTargetClass) throws Exception {

		logAdvice.reset();

		ProxyFactory factory = new ProxyFactory(target);
		factory.setProxyTargetClass(proxyTargetClass);
		factory.addAdvisor(advisor);
		TestService bean = (TestService) factory.getProxy();

		assertEquals(0, logAdvice.getCountThrows());
		try {
			bean.sayHello();
			fail("Expected exception");
		} catch (TestException e) {
			assertEquals(message, e.getMessage());
		}
		assertEquals(1, logAdvice.getCountThrows());
	}

	public static class SimpleThrowawayClassLoader extends OverridingClassLoader {

		/**
		 * Create a new SimpleThrowawayClassLoader for the given class loader.
		 * @param parent the ClassLoader to build a throwaway ClassLoader for
		 */
		public SimpleThrowawayClassLoader(ClassLoader parent) {
			super(parent);
		}

	}

	public static class TestException extends RuntimeException {

		public TestException(String string) {
			super(string);
		}

	}

	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Inherited
	public static @interface Log {
	}

	public static interface TestService {
		public String sayHello();
	}

	@Log
	public static class TestServiceImpl implements TestService {
		public String sayHello() {
			throw new TestException("TestServiceImpl");
		}
	}

	public class LogUserAdvice implements MethodBeforeAdvice, ThrowsAdvice {

		private int countBefore = 0;

		private int countThrows = 0;

		public void before(Method method, Object[] objects, Object o) throws Throwable {
			countBefore++;
		}

		public void afterThrowing(Exception e) throws Throwable {
			countThrows++;
			throw e;
		}

		public int getCountBefore() {
			return countBefore;
		}

		public int getCountThrows() {
			return countThrows;
		}

		public void reset() {
			countThrows = 0;
			countBefore = 0;
		}

	}

}
