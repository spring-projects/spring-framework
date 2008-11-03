/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.aspectj.annotation;

import java.lang.reflect.UndeclaredThrowableException;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;

import junit.framework.TestCase;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.DeclarePrecedence;
import org.aspectj.lang.annotation.Pointcut;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.annotation.ReflectiveAspectJAdvisorFactory.SyntheticInstantiationAdvisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.framework.Lockable;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Abstract tests for AspectJAdvisorFactory.
 * See subclasses for tests of concrete factories.
 *
 * @author Rod Johnson
 */
public abstract class AbstractAspectJAdvisorFactoryTests extends TestCase {
	
	/**
	 * To be overridden by concrete test subclasses.
	 * @return the fixture
	 */
	protected abstract AspectJAdvisorFactory getFixture();
	

	public void testRejectsPerCflowAspect() {
		try {
			getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(new PerCflowAspect(),"someBean"));
			fail("Cannot accept cflow");
		}
		catch (AopConfigException ex) {
			assertTrue(ex.getMessage().indexOf("PERCFLOW") != -1);
		}
	}
	
	public void testRejectsPerCflowBelowAspect() {
		try {
			getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(new PerCflowBelowAspect(),"someBean"));
			fail("Cannot accept cflowbelow");
		}
		catch (AopConfigException ex) {
			assertTrue(ex.getMessage().indexOf("PERCFLOWBELOW") != -1);
		}
	}

	public void testPerTargetAspect() throws SecurityException, NoSuchMethodException {
		TestBean target = new TestBean();
		int realAge = 65;
		target.setAge(realAge);
		TestBean itb = (TestBean) createProxy(target, 
				getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(new PerTargetAspect(), "someBean")),
				TestBean.class);
		assertEquals("Around advice must NOT apply", realAge, itb.getAge());
		
		Advised advised = (Advised) itb;
		SyntheticInstantiationAdvisor sia = (SyntheticInstantiationAdvisor) advised.getAdvisors()[1];
		assertTrue(sia.getPointcut().getMethodMatcher().matches(TestBean.class.getMethod("getSpouse"), null));
		InstantiationModelAwarePointcutAdvisorImpl imapa = (InstantiationModelAwarePointcutAdvisorImpl) advised.getAdvisors()[3];
		LazySingletonAspectInstanceFactoryDecorator maaif =
				(LazySingletonAspectInstanceFactoryDecorator) imapa.getAspectInstanceFactory();
		assertFalse(maaif.isMaterialized());

		// Check that the perclause pointcut is valid
		assertTrue(maaif.getAspectMetadata().getPerClausePointcut().getMethodMatcher().matches(TestBean.class.getMethod("getSpouse"), null));
		assertNotSame(imapa.getDeclaredPointcut(), imapa.getPointcut());
		
		// Hit the method in the per clause to instantiate the aspect
		itb.getSpouse();
		
		assertTrue(maaif.isMaterialized());

		assertEquals("Around advice must apply", 0, itb.getAge());
		assertEquals("Around advice must apply", 1, itb.getAge());
	}

	public void testMultiplePerTargetAspects() throws SecurityException, NoSuchMethodException {
		TestBean target = new TestBean();
		int realAge = 65;
		target.setAge(realAge);

		List<Advisor> advisors = new LinkedList<Advisor>();
		PerTargetAspect aspect1 = new PerTargetAspect();
		aspect1.count = 100;
		aspect1.setOrder(10);
		advisors.addAll(
				getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(aspect1, "someBean1")));
		PerTargetAspect aspect2 = new PerTargetAspect();
		aspect2.setOrder(5);
		advisors.addAll(
				getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(aspect2, "someBean2")));
		Collections.sort(advisors, new OrderComparator());

		TestBean itb = (TestBean) createProxy(target, advisors, TestBean.class);
		assertEquals("Around advice must NOT apply", realAge, itb.getAge());

		// Hit the method in the per clause to instantiate the aspect
		itb.getSpouse();

		assertEquals("Around advice must apply", 0, itb.getAge());
		assertEquals("Around advice must apply", 1, itb.getAge());
	}

	public void testMultiplePerTargetAspectsWithOrderAnnotation() throws SecurityException, NoSuchMethodException {
		TestBean target = new TestBean();
		int realAge = 65;
		target.setAge(realAge);

		List<Advisor> advisors = new LinkedList<Advisor>();
		PerTargetAspectWithOrderAnnotation10 aspect1 = new PerTargetAspectWithOrderAnnotation10();
		aspect1.count = 100;
		advisors.addAll(
				getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(aspect1, "someBean1")));
		PerTargetAspectWithOrderAnnotation5 aspect2 = new PerTargetAspectWithOrderAnnotation5();
		advisors.addAll(
				getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(aspect2, "someBean2")));
		Collections.sort(advisors, new OrderComparator());

		TestBean itb = (TestBean) createProxy(target, advisors, TestBean.class);
		assertEquals("Around advice must NOT apply", realAge, itb.getAge());

		// Hit the method in the per clause to instantiate the aspect
		itb.getSpouse();

		assertEquals("Around advice must apply", 0, itb.getAge());
		assertEquals("Around advice must apply", 1, itb.getAge());
	}

	public void testPerThisAspect() throws SecurityException, NoSuchMethodException {
		TestBean target = new TestBean();
		int realAge = 65;
		target.setAge(realAge);
		TestBean itb = (TestBean) createProxy(target, 
				getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(new PerThisAspect(), "someBean")),
				TestBean.class);
		assertEquals("Around advice must NOT apply", realAge, itb.getAge());
		
		Advised advised = (Advised) itb;
		// Will be ExposeInvocationInterceptor, synthetic instantiation advisor, 2 method advisors
		assertEquals(4, advised.getAdvisors().length);
		SyntheticInstantiationAdvisor sia = (SyntheticInstantiationAdvisor) advised.getAdvisors()[1];
		assertTrue(sia.getPointcut().getMethodMatcher().matches(TestBean.class.getMethod("getSpouse"), null));
		InstantiationModelAwarePointcutAdvisorImpl imapa = (InstantiationModelAwarePointcutAdvisorImpl) advised.getAdvisors()[2];
		LazySingletonAspectInstanceFactoryDecorator maaif =
				(LazySingletonAspectInstanceFactoryDecorator) imapa.getAspectInstanceFactory();
		assertFalse(maaif.isMaterialized());

		// Check that the perclause pointcut is valid
		assertTrue(maaif.getAspectMetadata().getPerClausePointcut().getMethodMatcher().matches(TestBean.class.getMethod("getSpouse"), null));
		assertNotSame(imapa.getDeclaredPointcut(), imapa.getPointcut());
		
		// Hit the method in the per clause to instantiate the aspect
		itb.getSpouse();
		
		assertTrue(maaif.isMaterialized());

		assertTrue(imapa.getDeclaredPointcut().getMethodMatcher().matches(TestBean.class.getMethod("getAge"), null));
	
		assertEquals("Around advice must apply", 0, itb.getAge());
		assertEquals("Around advice must apply", 1, itb.getAge());
	}
	
	public void testPerTypeWithinAspect() throws SecurityException, NoSuchMethodException {
		TestBean target = new TestBean();
		int realAge = 65;
		target.setAge(realAge);
		PerTypeWithinAspectInstanceFactory aif = new PerTypeWithinAspectInstanceFactory();
		TestBean itb = (TestBean) createProxy(target, 
				getFixture().getAdvisors(aif), 
				TestBean.class);
		assertEquals("No method calls", 0, aif.getInstantiationCount());
		assertEquals("Around advice must now apply", 0, itb.getAge());
		
		Advised advised = (Advised) itb;
		// Will be ExposeInvocationInterceptor, synthetic instantiation advisor, 2 method advisors
		assertEquals(4, advised.getAdvisors().length);
		SyntheticInstantiationAdvisor sia = (SyntheticInstantiationAdvisor) advised.getAdvisors()[1];
		assertTrue(sia.getPointcut().getMethodMatcher().matches(TestBean.class.getMethod("getSpouse"), null));
		InstantiationModelAwarePointcutAdvisorImpl imapa = (InstantiationModelAwarePointcutAdvisorImpl) advised.getAdvisors()[2];
		LazySingletonAspectInstanceFactoryDecorator maaif =
				(LazySingletonAspectInstanceFactoryDecorator) imapa.getAspectInstanceFactory();
		assertTrue(maaif.isMaterialized());

		// Check that the perclause pointcut is valid
		assertTrue(maaif.getAspectMetadata().getPerClausePointcut().getMethodMatcher().matches(TestBean.class.getMethod("getSpouse"), null));
		assertNotSame(imapa.getDeclaredPointcut(), imapa.getPointcut());
		
		// Hit the method in the per clause to instantiate the aspect
		itb.getSpouse();
		
		assertTrue(maaif.isMaterialized());

		assertTrue(imapa.getDeclaredPointcut().getMethodMatcher().matches(TestBean.class.getMethod("getAge"), null));
	
		assertEquals("Around advice must still apply", 1, itb.getAge());
		assertEquals("Around advice must still apply", 2, itb.getAge());
		
		TestBean itb2 = (TestBean) createProxy(target, 
				getFixture().getAdvisors(aif), 
				TestBean.class);
		assertEquals(1, aif.getInstantiationCount());
		assertEquals("Around advice be independent for second instance", 0, itb2.getAge());
		assertEquals(2, aif.getInstantiationCount());
	}

	public void testNamedPointcutAspectWithFQN() {
		testNamedPointcuts(new NamedPointcutAspectWithFQN());
	}

	public void testNamedPointcutAspectWithoutFQN() {
		testNamedPointcuts(new NamedPointcutAspectWithoutFQN());
	}

	public void testNamedPointcutFromAspectLibrary() {
		testNamedPointcuts(new NamedPointcutAspectFromLibrary());
	}

	public void testNamedPointcutFromAspectLibraryWithBinding() {
		TestBean target = new TestBean();
		ITestBean itb = (ITestBean) createProxy(target, 
				getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(new NamedPointcutAspectFromLibraryWithBinding(),"someBean")), 
				ITestBean.class);
		itb.setAge(10);
		assertEquals("Around advice must apply", 20, itb.getAge());
		assertEquals(20,target.getAge());
	}
	
	private void testNamedPointcuts(Object aspectInstance) {
		TestBean target = new TestBean();
		int realAge = 65;
		target.setAge(realAge);
		ITestBean itb = (ITestBean) createProxy(target, 
				getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(aspectInstance,"someBean")), 
				ITestBean.class);
		assertEquals("Around advice must apply", -1, itb.getAge());
		assertEquals(realAge, target.getAge());
	}

	public void testBindingWithSingleArg() {
		TestBean target = new TestBean();
		ITestBean itb = (ITestBean) createProxy(target, 
				getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(new BindingAspectWithSingleArg(),"someBean")), 
				ITestBean.class);
		itb.setAge(10);
		assertEquals("Around advice must apply", 20, itb.getAge());
		assertEquals(20,target.getAge());
	}

	public void testBindingWithMultipleArgsDifferentlyOrdered() {
		ManyValuedArgs target = new ManyValuedArgs();
		ManyValuedArgs mva = (ManyValuedArgs) createProxy(target, 
				getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(new ManyValuedArgs(),"someBean")), 
				ManyValuedArgs.class);
		
		String a = "a";
		int b = 12;
		int c = 25;
		String d = "d";
		StringBuffer e = new StringBuffer("stringbuf");
		String expectedResult = a + b+ c + d + e;
		assertEquals(expectedResult, mva.mungeArgs(a, b, c, d, e));
	}
	
	/**
	 * In this case the introduction will be made.
	 */
	public void testIntroductionOnTargetNotImplementingInterface() {
		NotLockable notLockableTarget = new NotLockable();
		assertFalse(notLockableTarget instanceof Lockable);
		NotLockable notLockable1 = (NotLockable) createProxy(notLockableTarget,
				getFixture().getAdvisors(
						new SingletonMetadataAwareAspectInstanceFactory(new MakeLockable(),"someBean")),
				NotLockable.class);
		assertTrue(notLockable1 instanceof Lockable);
		Lockable lockable = (Lockable) notLockable1;
		assertFalse(lockable.locked());
		lockable.lock();
		assertTrue(lockable.locked());
		
		NotLockable notLockable2Target = new NotLockable();
		NotLockable notLockable2 = (NotLockable) createProxy(notLockable2Target,
				getFixture().getAdvisors(
						new SingletonMetadataAwareAspectInstanceFactory(new MakeLockable(),"someBean")),
				NotLockable.class);
		assertTrue(notLockable2 instanceof Lockable);
		Lockable lockable2 = (Lockable) notLockable2;
		assertFalse(lockable2.locked());
		notLockable2.setIntValue(1);
		lockable2.lock();
		try {
			notLockable2.setIntValue(32);
			fail();
		}
		catch (IllegalStateException ex) {
		}
		assertTrue(lockable2.locked());
	}
	
	public void testIntroductionAdvisorExcludedFromTargetImplementingInterface() {
		assertTrue(AopUtils.findAdvisorsThatCanApply(
						getFixture().getAdvisors(
									new SingletonMetadataAwareAspectInstanceFactory(
											new MakeLockable(),"someBean")), 
						CannotBeUnlocked.class).isEmpty());
		assertEquals(2, AopUtils.findAdvisorsThatCanApply(getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(new MakeLockable(),"someBean")), NotLockable.class).size());
	}
	
	@SuppressWarnings("unchecked")
	public void testIntroductionOnTargetImplementingInterface() {
		CannotBeUnlocked target = new CannotBeUnlocked();
		Lockable proxy = (Lockable) createProxy(target,
				// Ensure that we exclude
				AopUtils.findAdvisorsThatCanApply(
						getFixture().getAdvisors(
								new SingletonMetadataAwareAspectInstanceFactory(new MakeLockable(), "someBean")),
						CannotBeUnlocked.class
				),
				CannotBeUnlocked.class);
		assertTrue(proxy instanceof Lockable);
		Lockable lockable = (Lockable) proxy;
		assertTrue("Already locked", lockable.locked());
		lockable.lock();
		assertTrue("Real target ignores locking", lockable.locked());
		try {
			lockable.unlock();
			fail();
		}
		catch (UnsupportedOperationException ex) {
			// Ok
		}
	}
	
	@SuppressWarnings("unchecked")
	public void testIntroductionOnTargetExcludedByTypePattern() {
		LinkedList target = new LinkedList();
		List proxy = (List) createProxy(target,
				AopUtils.findAdvisorsThatCanApply(
						getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(new MakeLockable(), "someBean")),
						List.class
				),
				CannotBeUnlocked.class);
		assertFalse("Type pattern must have excluded mixin", proxy instanceof Lockable);
	}

	// TODO: Why does this test fail? It hasn't been run before, so it maybe never actually passed...
	public void XtestIntroductionWithArgumentBinding() {
		TestBean target = new TestBean();
		
		List<Advisor> advisors = getFixture().getAdvisors(
				new SingletonMetadataAwareAspectInstanceFactory(new MakeITestBeanModifiable(),"someBean"));
		advisors.addAll(getFixture().getAdvisors(
				new SingletonMetadataAwareAspectInstanceFactory(new MakeLockable(),"someBean")));
		
		Modifiable modifiable = (Modifiable) createProxy(target,
				advisors,
				ITestBean.class);
		assertTrue(modifiable instanceof Modifiable);
		Lockable lockable = (Lockable) modifiable;
		assertFalse(lockable.locked());
		
		ITestBean itb = (ITestBean) modifiable;
		assertFalse(modifiable.isModified());
		int oldAge = itb.getAge();
		itb.setAge(oldAge + 1);
		assertTrue(modifiable.isModified());
		modifiable.acceptChanges();
		assertFalse(modifiable.isModified());
		itb.setAge(itb.getAge());
		assertFalse("Setting same value does not modify", modifiable.isModified());
		itb.setName("And now for something completely different");
		assertTrue(modifiable.isModified());
		
		lockable.lock();
		assertTrue(lockable.locked());
		try {
			itb.setName("Else");
			fail("Should be locked");
		}
		catch (IllegalStateException ex) {
			// Ok
		}
		lockable.unlock();
		itb.setName("Tony");
	}

	public void testAspectMethodThrowsExceptionLegalOnSignature() {
		TestBean target = new TestBean();
		UnsupportedOperationException expectedException = new UnsupportedOperationException();
		List<Advisor> advisors = getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(new ExceptionAspect(expectedException),"someBean"));
		assertEquals("One advice method was found", 1, advisors.size());
		ITestBean itb = (ITestBean) createProxy(target, 
				advisors, 
				ITestBean.class);
		try {
			itb.getAge();
			fail();
		}
		catch (UnsupportedOperationException ex) {
			assertSame(expectedException, ex);
		}
	}
	
	// TODO document this behaviour.
	// Is it different AspectJ behaviour, at least for checked exceptions?
	public void testAspectMethodThrowsExceptionIllegalOnSignature() {
		TestBean target = new TestBean();
		RemoteException expectedException = new RemoteException();
		List<Advisor> advisors = getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(new ExceptionAspect(expectedException),"someBean"));
		assertEquals("One advice method was found", 1, advisors.size());
		ITestBean itb = (ITestBean) createProxy(target, 
				advisors, 
				ITestBean.class);
		try {
			itb.getAge();
			fail();
		}
		catch (UndeclaredThrowableException ex) {
			assertSame(expectedException, ex.getCause());
		}
	}
	
	protected Object createProxy(Object target, List advisors, Class ... interfaces) {
		ProxyFactory pf = new ProxyFactory(target);
		if (interfaces.length > 1 || interfaces[0].isInterface()) {
			pf.setInterfaces(interfaces);
		}
		else {
			pf.setProxyTargetClass(true);
		}

		// Required everywhere we use AspectJ proxies
		pf.addAdvice(ExposeInvocationInterceptor.INSTANCE);

		for (Object a : advisors) {
			pf.addAdvisor((Advisor) a);
		}

		pf.setExposeProxy(true);
		return pf.getProxy();
	}

	public void testTwoAdvicesOnOneAspect() {
		TestBean target = new TestBean();

		TwoAdviceAspect twoAdviceAspect = new TwoAdviceAspect();
		List<Advisor> advisors = getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(twoAdviceAspect,"someBean"));
		assertEquals("Two advice methods found", 2, advisors.size());
		ITestBean itb = (ITestBean) createProxy(target, 
				advisors, 
				ITestBean.class);
		itb.setName("");
		assertEquals(0, itb.getAge());
		int newAge = 32;
		itb.setAge(newAge);
		assertEquals(1, itb.getAge());
	}

	public void testAfterAdviceTypes() throws Exception {
		Echo target = new Echo();

		ExceptionHandling afterReturningAspect = new ExceptionHandling();
		List<Advisor> advisors = getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(afterReturningAspect,"someBean"));
		Echo echo = (Echo) createProxy(target, 
				advisors, 
				Echo.class);
		assertEquals(0, afterReturningAspect.successCount);
		assertEquals("", echo.echo(""));
		assertEquals(1, afterReturningAspect.successCount);
		assertEquals(0, afterReturningAspect.failureCount);
		try {
			echo.echo(new ServletException());
			fail();
		}
		catch (ServletException ex) {
			// Ok
		}
		catch (Exception ex) {
			fail();
		}
		assertEquals(1, afterReturningAspect.successCount);
		assertEquals(1, afterReturningAspect.failureCount);
		assertEquals(afterReturningAspect.failureCount + afterReturningAspect.successCount, afterReturningAspect.afterCount);
	}

	public void testFailureWithoutExplicitDeclarePrecedence() {
		TestBean target = new TestBean();
		ITestBean itb = (ITestBean) createProxy(target, 
				getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(new NoDeclarePrecedenceShouldFail(), "someBean")),
				ITestBean.class);
		try {
			itb.getAge();
			fail();
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}
	
	public void testDeclarePrecedenceNotSupported() {
		TestBean target = new TestBean();
		try {
			createProxy(target, 
				getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(
						new DeclarePrecedenceShouldSucceed(),"someBean")), 
				ITestBean.class);
			fail();
		}
		catch (IllegalArgumentException ex) {
			// Not supported in 2.0
		}
	}

	/** Not supported in 2.0!
	public void testExplicitDeclarePrecedencePreventsFailure() {
		TestBean target = new TestBean();
		ITestBean itb = (ITestBean) createProxy(target,
				getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(new DeclarePrecedenceShouldSucceed(), "someBean")),
				ITestBean.class);
		assertEquals(666, itb.getAge());
	}
	*/


	@Aspect("percflow(execution(* *(..)))")
	public static class PerCflowAspect {
	}


	@Aspect("percflowbelow(execution(* *(..)))")
	public static class PerCflowBelowAspect {
	}


	@Aspect("pertarget(execution(* *.getSpouse()))")
	public static class PerTargetAspect implements Ordered {

		public int count;

		private int order = Ordered.LOWEST_PRECEDENCE;

		@Around("execution(int *.getAge())")
		public int returnCountAsAge() {
			return count++;
		}

		@Before("execution(void *.set*(int))")
		public void countSetter() {
			++count;
		}

		public int getOrder() {
			return this.order;
		}

		public void setOrder(int order) {
			this.order = order;
		}
	}


	@Aspect("pertarget(execution(* *.getSpouse()))")
	@Order(10)
	public static class PerTargetAspectWithOrderAnnotation10 {

		public int count;

		@Around("execution(int *.getAge())")
		public int returnCountAsAge() {
			return count++;
		}

		@Before("execution(void *.set*(int))")
		public void countSetter() {
			++count;
		}
	}


	@Aspect("pertarget(execution(* *.getSpouse()))")
	@Order(5)
	public static class PerTargetAspectWithOrderAnnotation5 {

		public int count;

		@Around("execution(int *.getAge())")
		public int returnCountAsAge() {
			return count++;
		}

		@Before("execution(void *.set*(int))")
		public void countSetter() {
			++count;
		}
	}


	@Aspect("perthis(execution(* *.getSpouse()))")
	public static class PerThisAspect {

		public int count;

		/**
		 * Just to check that this doesn't cause problems with introduction processing
		 */
		private ITestBean fieldThatShouldBeIgnoredBySpringAtAspectJProcessing = new TestBean();

		@Around("execution(int *.getAge())")
		public int returnCountAsAge() {
			return count++;
		}

		@Before("execution(void *.set*(int))")
		public void countSetter() {
			++count;
		}
	}


	@Aspect("pertypewithin(org.springframework.beans.IOther+)")
	public static class PerTypeWithinAspect {

		public int count;

		@Around("execution(int *.getAge())")
		public int returnCountAsAge() {
			return count++;
		}

		@Before("execution(void *.*(..))")
		public void countAnythingVoid() {
			++count;
		}
	}


	private class PerTypeWithinAspectInstanceFactory implements MetadataAwareAspectInstanceFactory {

		private int count;

		public int getInstantiationCount() {
			return this.count;
		}

		public Object getAspectInstance() {
			++this.count;
			return new PerTypeWithinAspect();
		}

		public ClassLoader getAspectClassLoader() {
			return PerTypeWithinAspect.class.getClassLoader();
		}

		public AspectMetadata getAspectMetadata() {
			return new AspectMetadata(PerTypeWithinAspect.class, "perTypeWithin");
		}

		public int getOrder() {
			return Ordered.LOWEST_PRECEDENCE;
		}
	}


	@Aspect
	public static class NamedPointcutAspectWithFQN {

		@SuppressWarnings("unused")
		private ITestBean fieldThatShouldBeIgnoredBySpringAtAspectJProcessing = new TestBean();

		@Pointcut("execution(* getAge())")
		public void getAge() {
		}

		@Around("org.springframework.aop.aspectj.annotation.AbstractAspectJAdvisorFactoryTests.NamedPointcutAspectWithFQN.getAge()")
		public int changeReturnValue(ProceedingJoinPoint pjp) {
			return -1;
		}
	}


	@Aspect
	public static class NamedPointcutAspectWithoutFQN {
		@Pointcut("execution(* getAge())")
		public void getAge() {
		}

		@Around("getAge()")
		public int changeReturnValue(ProceedingJoinPoint pjp) {
			return -1;
		}
	}


	@Aspect
	public static class NamedPointcutAspectFromLibrary {

		@Around("org.springframework.aop.aspectj.annotation.AbstractAspectJAdvisorFactoryTests.Library.propertyAccess()")
		public int changeReturnType(ProceedingJoinPoint pjp) {
			return -1;
		}

		@Around(value="org.springframework.aop.aspectj.annotation.AbstractAspectJAdvisorFactoryTests.Library.integerArgOperation(x)", argNames="x")
		public void doubleArg(ProceedingJoinPoint pjp, int x) throws Throwable {
			pjp.proceed(new Object[] {x*2});
		}
	}


	@Aspect
	public static class Library {

		@Pointcut("execution(!void get*())")
		public void propertyAccess() {}

		@Pointcut("execution(* *(..)) && args(i)")
		public void integerArgOperation(int i) {}

	}


	@Aspect
	public static class NamedPointcutAspectFromLibraryWithBinding {

		@Around(value="org.springframework.aop.aspectj.annotation.AbstractAspectJAdvisorFactoryTests.Library.integerArgOperation(x)", argNames="x")
		public void doubleArg(ProceedingJoinPoint pjp, int x) throws Throwable {
			pjp.proceed(new Object[] {x*2});
		}
	}


	@Aspect
	public static class BindingAspectWithSingleArg {

		@Pointcut(value="args(a)", argNames="a")
		public void setAge(int a) {}

		@Around(value="setAge(age)",argNames="age")
		// @ArgNames({"age"})  // AMC needs more work here? ignoring pjp arg... ok??
		//                     // argNames should be suported in Around as it is in Pointcut
		public void changeReturnType(ProceedingJoinPoint pjp, int age) throws Throwable {
			pjp.proceed(new Object[] {age*2});
		}
	}


	@Aspect
	public static class ManyValuedArgs {
		public String mungeArgs(String a, int b, int c, String d, StringBuffer e) {
			return a + b + c + d + e;
		}

		@Around(value="execution(String mungeArgs(..)) && args(a, b, c, d, e)",
				argNames="b,c,d,e,a")
		public String reverseAdvice(ProceedingJoinPoint pjp, int b, int c, String d, StringBuffer e, String a) throws Throwable {
			assertEquals(a + b+ c+ d+ e, pjp.proceed());
			return a + b + c + d + e;
		}
	}


	@Aspect
	public static class ExceptionAspect {
		private final Exception ex;

		public ExceptionAspect(Exception ex) {
			this.ex = ex;
		}

		@Before("execution(* getAge())")
		public void throwException() throws Exception {
			throw ex;
		}
	}


	@Aspect
	public static class TwoAdviceAspect {
		private int totalCalls;

		@Around("execution(* getAge())")
		public int returnCallCount(ProceedingJoinPoint pjp) throws Exception {
			return totalCalls;
		}

		@Before("execution(* setAge(int)) && args(newAge)")
		public void countSet(int newAge) throws Exception {
			++totalCalls;
		}
	}


	public static class Echo {

		public Object echo(Object o) throws Exception {
			if (o instanceof Exception) {
				throw (Exception) o;
			}
			return o;
		}
	}


	@Aspect
	public static class ExceptionHandling {
		public int successCount;
		public int failureCount;
		public int afterCount;

		@AfterReturning("execution(* echo(*))")
		public void succeeded() {
			++successCount;
		}

		@AfterThrowing("execution(* echo(*))")
		public void failed() {
			++failureCount;
		}

		@After("execution(* echo(*))")
		public void invoked() {
			++afterCount;
		}
	}


	@Aspect
	public static class NoDeclarePrecedenceShouldFail {

		@Pointcut("execution(int *.getAge())")
		public void getAge() {
		}

		@Before("getAge()")
		public void blowUpButDoesntMatterBecauseAroundAdviceWontLetThisBeInvoked() {
			throw new IllegalStateException();
		}

		@Around("getAge()")
		public int preventExecution(ProceedingJoinPoint pjp) {
			return 666;
		}
	}


	@Aspect
	@DeclarePrecedence("org.springframework..*")
	public static class DeclarePrecedenceShouldSucceed {

		@Pointcut("execution(int *.getAge())")
		public void getAge() {
		}

		@Before("getAge()")
		public void blowUpButDoesntMatterBecauseAroundAdviceWontLetThisBeInvoked() {
			throw new IllegalStateException();
		}

		@Around("getAge()")
		public int preventExecution(ProceedingJoinPoint pjp) {
			return 666;
		}
	}

}
