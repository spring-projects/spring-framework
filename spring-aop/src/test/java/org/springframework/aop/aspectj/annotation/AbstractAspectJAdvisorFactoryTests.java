/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.aspectj.annotation;

import java.io.FileNotFoundException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.DeclareParents;
import org.aspectj.lang.annotation.DeclarePrecedence;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import test.aop.DefaultLockable;
import test.aop.Lockable;
import test.aop.PerTargetAspect;
import test.aop.TwoAdviceAspect;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Abstract tests for AspectJAdvisorFactory.
 * See subclasses for tests of concrete factories.
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @author Phillip Webb
 */
public abstract class AbstractAspectJAdvisorFactoryTests {

	/**
	 * To be overridden by concrete test subclasses.
	 * @return the fixture
	 */
	protected abstract AspectJAdvisorFactory getFixture();


	@Test
	public void testRejectsPerCflowAspect() {
		assertThatExceptionOfType(AopConfigException.class).isThrownBy(() ->
				getFixture().getAdvisors(
						new SingletonMetadataAwareAspectInstanceFactory(new PerCflowAspect(), "someBean")))
			.withMessageContaining("PERCFLOW");
	}

	@Test
	public void testRejectsPerCflowBelowAspect() {
		assertThatExceptionOfType(AopConfigException.class).isThrownBy(() ->
					getFixture().getAdvisors(
							new SingletonMetadataAwareAspectInstanceFactory(new PerCflowBelowAspect(), "someBean")))
			.withMessageContaining("PERCFLOWBELOW");
	}

	@Test
	public void testPerTargetAspect() throws SecurityException, NoSuchMethodException {
		TestBean target = new TestBean();
		int realAge = 65;
		target.setAge(realAge);
		TestBean itb = (TestBean) createProxy(target,
				getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(new PerTargetAspect(), "someBean")),
				TestBean.class);
		assertThat(itb.getAge()).as("Around advice must NOT apply").isEqualTo(realAge);

		Advised advised = (Advised) itb;
		ReflectiveAspectJAdvisorFactory.SyntheticInstantiationAdvisor sia =
				(ReflectiveAspectJAdvisorFactory.SyntheticInstantiationAdvisor) advised.getAdvisors()[1];
		assertThat(sia.getPointcut().getMethodMatcher().matches(TestBean.class.getMethod("getSpouse"), null)).isTrue();
		InstantiationModelAwarePointcutAdvisorImpl imapa = (InstantiationModelAwarePointcutAdvisorImpl) advised.getAdvisors()[3];
		LazySingletonAspectInstanceFactoryDecorator maaif =
				(LazySingletonAspectInstanceFactoryDecorator) imapa.getAspectInstanceFactory();
		assertThat(maaif.isMaterialized()).isFalse();

		// Check that the perclause pointcut is valid
		assertThat(maaif.getAspectMetadata().getPerClausePointcut().getMethodMatcher().matches(TestBean.class.getMethod("getSpouse"), null)).isTrue();
		assertThat(imapa.getPointcut()).isNotSameAs(imapa.getDeclaredPointcut());

		// Hit the method in the per clause to instantiate the aspect
		itb.getSpouse();

		assertThat(maaif.isMaterialized()).isTrue();

		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(0);
		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(1);
	}

	@Test
	public void testMultiplePerTargetAspects() throws SecurityException, NoSuchMethodException {
		TestBean target = new TestBean();
		int realAge = 65;
		target.setAge(realAge);

		List<Advisor> advisors = new LinkedList<>();
		PerTargetAspect aspect1 = new PerTargetAspect();
		aspect1.count = 100;
		aspect1.setOrder(10);
		advisors.addAll(
				getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(aspect1, "someBean1")));
		PerTargetAspect aspect2 = new PerTargetAspect();
		aspect2.setOrder(5);
		advisors.addAll(
				getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(aspect2, "someBean2")));
		OrderComparator.sort(advisors);

		TestBean itb = (TestBean) createProxy(target, advisors, TestBean.class);
		assertThat(itb.getAge()).as("Around advice must NOT apply").isEqualTo(realAge);

		// Hit the method in the per clause to instantiate the aspect
		itb.getSpouse();

		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(0);
		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(1);
	}

	@Test
	public void testMultiplePerTargetAspectsWithOrderAnnotation() throws SecurityException, NoSuchMethodException {
		TestBean target = new TestBean();
		int realAge = 65;
		target.setAge(realAge);

		List<Advisor> advisors = new LinkedList<>();
		PerTargetAspectWithOrderAnnotation10 aspect1 = new PerTargetAspectWithOrderAnnotation10();
		aspect1.count = 100;
		advisors.addAll(
				getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(aspect1, "someBean1")));
		PerTargetAspectWithOrderAnnotation5 aspect2 = new PerTargetAspectWithOrderAnnotation5();
		advisors.addAll(
				getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(aspect2, "someBean2")));
		OrderComparator.sort(advisors);

		TestBean itb = (TestBean) createProxy(target, advisors, TestBean.class);
		assertThat(itb.getAge()).as("Around advice must NOT apply").isEqualTo(realAge);

		// Hit the method in the per clause to instantiate the aspect
		itb.getSpouse();

		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(0);
		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(1);
	}

	@Test
	public void testPerThisAspect() throws SecurityException, NoSuchMethodException {
		TestBean target = new TestBean();
		int realAge = 65;
		target.setAge(realAge);
		TestBean itb = (TestBean) createProxy(target,
				getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(new PerThisAspect(), "someBean")),
				TestBean.class);
		assertThat(itb.getAge()).as("Around advice must NOT apply").isEqualTo(realAge);

		Advised advised = (Advised) itb;
		// Will be ExposeInvocationInterceptor, synthetic instantiation advisor, 2 method advisors
		assertThat(advised.getAdvisors().length).isEqualTo(4);
		ReflectiveAspectJAdvisorFactory.SyntheticInstantiationAdvisor sia =
				(ReflectiveAspectJAdvisorFactory.SyntheticInstantiationAdvisor) advised.getAdvisors()[1];
		assertThat(sia.getPointcut().getMethodMatcher().matches(TestBean.class.getMethod("getSpouse"), null)).isTrue();
		InstantiationModelAwarePointcutAdvisorImpl imapa = (InstantiationModelAwarePointcutAdvisorImpl) advised.getAdvisors()[2];
		LazySingletonAspectInstanceFactoryDecorator maaif =
				(LazySingletonAspectInstanceFactoryDecorator) imapa.getAspectInstanceFactory();
		assertThat(maaif.isMaterialized()).isFalse();

		// Check that the perclause pointcut is valid
		assertThat(maaif.getAspectMetadata().getPerClausePointcut().getMethodMatcher().matches(TestBean.class.getMethod("getSpouse"), null)).isTrue();
		assertThat(imapa.getPointcut()).isNotSameAs(imapa.getDeclaredPointcut());

		// Hit the method in the per clause to instantiate the aspect
		itb.getSpouse();

		assertThat(maaif.isMaterialized()).isTrue();

		assertThat(imapa.getDeclaredPointcut().getMethodMatcher().matches(TestBean.class.getMethod("getAge"), null)).isTrue();

		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(0);
		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(1);
	}

	@Test
	public void testPerTypeWithinAspect() throws SecurityException, NoSuchMethodException {
		TestBean target = new TestBean();
		int realAge = 65;
		target.setAge(realAge);
		PerTypeWithinAspectInstanceFactory aif = new PerTypeWithinAspectInstanceFactory();
		TestBean itb = (TestBean) createProxy(target, getFixture().getAdvisors(aif), TestBean.class);
		assertThat(aif.getInstantiationCount()).as("No method calls").isEqualTo(0);
		assertThat(itb.getAge()).as("Around advice must now apply").isEqualTo(0);

		Advised advised = (Advised) itb;
		// Will be ExposeInvocationInterceptor, synthetic instantiation advisor, 2 method advisors
		assertThat(advised.getAdvisors().length).isEqualTo(4);
		ReflectiveAspectJAdvisorFactory.SyntheticInstantiationAdvisor sia =
				(ReflectiveAspectJAdvisorFactory.SyntheticInstantiationAdvisor) advised.getAdvisors()[1];
		assertThat(sia.getPointcut().getMethodMatcher().matches(TestBean.class.getMethod("getSpouse"), null)).isTrue();
		InstantiationModelAwarePointcutAdvisorImpl imapa = (InstantiationModelAwarePointcutAdvisorImpl) advised.getAdvisors()[2];
		LazySingletonAspectInstanceFactoryDecorator maaif =
				(LazySingletonAspectInstanceFactoryDecorator) imapa.getAspectInstanceFactory();
		assertThat(maaif.isMaterialized()).isTrue();

		// Check that the perclause pointcut is valid
		assertThat(maaif.getAspectMetadata().getPerClausePointcut().getMethodMatcher().matches(TestBean.class.getMethod("getSpouse"), null)).isTrue();
		assertThat(imapa.getPointcut()).isNotSameAs(imapa.getDeclaredPointcut());

		// Hit the method in the per clause to instantiate the aspect
		itb.getSpouse();

		assertThat(maaif.isMaterialized()).isTrue();

		assertThat(imapa.getDeclaredPointcut().getMethodMatcher().matches(TestBean.class.getMethod("getAge"), null)).isTrue();

		assertThat(itb.getAge()).as("Around advice must still apply").isEqualTo(1);
		assertThat(itb.getAge()).as("Around advice must still apply").isEqualTo(2);

		TestBean itb2 = (TestBean) createProxy(target, getFixture().getAdvisors(aif), TestBean.class);
		assertThat(aif.getInstantiationCount()).isEqualTo(1);
		assertThat(itb2.getAge()).as("Around advice be independent for second instance").isEqualTo(0);
		assertThat(aif.getInstantiationCount()).isEqualTo(2);
	}

	@Test
	public void testNamedPointcutAspectWithFQN() {
		testNamedPointcuts(new NamedPointcutAspectWithFQN());
	}

	@Test
	public void testNamedPointcutAspectWithoutFQN() {
		testNamedPointcuts(new NamedPointcutAspectWithoutFQN());
	}

	@Test
	public void testNamedPointcutFromAspectLibrary() {
		testNamedPointcuts(new NamedPointcutAspectFromLibrary());
	}

	@Test
	public void testNamedPointcutFromAspectLibraryWithBinding() {
		TestBean target = new TestBean();
		ITestBean itb = (ITestBean) createProxy(target,
				getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(
						new NamedPointcutAspectFromLibraryWithBinding(), "someBean")),
				ITestBean.class);
		itb.setAge(10);
		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(20);
		assertThat(target.getAge()).isEqualTo(20);
	}

	private void testNamedPointcuts(Object aspectInstance) {
		TestBean target = new TestBean();
		int realAge = 65;
		target.setAge(realAge);
		ITestBean itb = (ITestBean) createProxy(target,
				getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(aspectInstance, "someBean")),
				ITestBean.class);
		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(-1);
		assertThat(target.getAge()).isEqualTo(realAge);
	}

	@Test
	public void testBindingWithSingleArg() {
		TestBean target = new TestBean();
		ITestBean itb = (ITestBean) createProxy(target,
				getFixture().getAdvisors(
						new SingletonMetadataAwareAspectInstanceFactory(new BindingAspectWithSingleArg(), "someBean")),
				ITestBean.class);
		itb.setAge(10);
		assertThat(itb.getAge()).as("Around advice must apply").isEqualTo(20);
		assertThat(target.getAge()).isEqualTo(20);
	}

	@Test
	public void testBindingWithMultipleArgsDifferentlyOrdered() {
		ManyValuedArgs target = new ManyValuedArgs();
		ManyValuedArgs mva = (ManyValuedArgs) createProxy(target,
				getFixture().getAdvisors(
						new SingletonMetadataAwareAspectInstanceFactory(new ManyValuedArgs(), "someBean")),
				ManyValuedArgs.class);

		String a = "a";
		int b = 12;
		int c = 25;
		String d = "d";
		StringBuffer e = new StringBuffer("stringbuf");
		String expectedResult = a + b+ c + d + e;
		assertThat(mva.mungeArgs(a, b, c, d, e)).isEqualTo(expectedResult);
	}

	/**
	 * In this case the introduction will be made.
	 */
	@Test
	public void testIntroductionOnTargetNotImplementingInterface() {
		NotLockable notLockableTarget = new NotLockable();
		assertThat(notLockableTarget instanceof Lockable).isFalse();
		NotLockable notLockable1 = (NotLockable) createProxy(notLockableTarget,
				getFixture().getAdvisors(
						new SingletonMetadataAwareAspectInstanceFactory(new MakeLockable(), "someBean")),
				NotLockable.class);
		assertThat(notLockable1 instanceof Lockable).isTrue();
		Lockable lockable = (Lockable) notLockable1;
		assertThat(lockable.locked()).isFalse();
		lockable.lock();
		assertThat(lockable.locked()).isTrue();

		NotLockable notLockable2Target = new NotLockable();
		NotLockable notLockable2 = (NotLockable) createProxy(notLockable2Target,
				getFixture().getAdvisors(
						new SingletonMetadataAwareAspectInstanceFactory(new MakeLockable(), "someBean")),
				NotLockable.class);
		assertThat(notLockable2 instanceof Lockable).isTrue();
		Lockable lockable2 = (Lockable) notLockable2;
		assertThat(lockable2.locked()).isFalse();
		notLockable2.setIntValue(1);
		lockable2.lock();
		assertThatIllegalStateException().isThrownBy(() ->
			notLockable2.setIntValue(32));
		assertThat(lockable2.locked()).isTrue();
	}

	@Test
	public void testIntroductionAdvisorExcludedFromTargetImplementingInterface() {
		assertThat(AopUtils.findAdvisorsThatCanApply(
		getFixture().getAdvisors(
				new SingletonMetadataAwareAspectInstanceFactory(new MakeLockable(), "someBean")),
		CannotBeUnlocked.class).isEmpty()).isTrue();
		assertThat(AopUtils.findAdvisorsThatCanApply(getFixture().getAdvisors(
		new SingletonMetadataAwareAspectInstanceFactory(new MakeLockable(),"someBean")), NotLockable.class).size()).isEqualTo(2);
	}

	@Test
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
		assertThat(proxy).isInstanceOf(Lockable.class);
		Lockable lockable = proxy;
		assertThat(lockable.locked()).as("Already locked").isTrue();
		lockable.lock();
		assertThat(lockable.locked()).as("Real target ignores locking").isTrue();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				lockable.unlock());
	}

	@Test
	public void testIntroductionOnTargetExcludedByTypePattern() {
		LinkedList<Object> target = new LinkedList<>();
		List<?> proxy = (List<?>) createProxy(target,
				AopUtils.findAdvisorsThatCanApply(
						getFixture().getAdvisors(new SingletonMetadataAwareAspectInstanceFactory(new MakeLockable(), "someBean")),
						List.class
				),
				List.class);
		assertThat(proxy instanceof Lockable).as("Type pattern must have excluded mixin").isFalse();
	}

	@Test
	public void testIntroductionBasedOnAnnotationMatch_SPR5307() {
		AnnotatedTarget target = new AnnotatedTargetImpl();
		List<Advisor> advisors = getFixture().getAdvisors(
				new SingletonMetadataAwareAspectInstanceFactory(new MakeAnnotatedTypeModifiable(), "someBean"));
		Object proxy = createProxy(target, advisors, AnnotatedTarget.class);
		System.out.println(advisors.get(1));
		assertThat(proxy instanceof Lockable).isTrue();
		Lockable lockable = (Lockable)proxy;
		lockable.locked();
	}

	// TODO: Why does this test fail? It hasn't been run before, so it maybe never actually passed...
	@Test
	@Disabled
	public void testIntroductionWithArgumentBinding() {
		TestBean target = new TestBean();

		List<Advisor> advisors = getFixture().getAdvisors(
				new SingletonMetadataAwareAspectInstanceFactory(new MakeITestBeanModifiable(), "someBean"));
		advisors.addAll(getFixture().getAdvisors(
				new SingletonMetadataAwareAspectInstanceFactory(new MakeLockable(), "someBean")));

		Modifiable modifiable = (Modifiable) createProxy(target, advisors, ITestBean.class);
		assertThat(modifiable).isInstanceOf(Modifiable.class);
		Lockable lockable = (Lockable) modifiable;
		assertThat(lockable.locked()).isFalse();

		ITestBean itb = (ITestBean) modifiable;
		assertThat(modifiable.isModified()).isFalse();
		int oldAge = itb.getAge();
		itb.setAge(oldAge + 1);
		assertThat(modifiable.isModified()).isTrue();
		modifiable.acceptChanges();
		assertThat(modifiable.isModified()).isFalse();
		itb.setAge(itb.getAge());
		assertThat(modifiable.isModified()).as("Setting same value does not modify").isFalse();
		itb.setName("And now for something completely different");
		assertThat(modifiable.isModified()).isTrue();

		lockable.lock();
		assertThat(lockable.locked()).isTrue();
		assertThatIllegalStateException().as("Should be locked").isThrownBy(() ->
				itb.setName("Else"));
		lockable.unlock();
		itb.setName("Tony");
	}

	@Test
	public void testAspectMethodThrowsExceptionLegalOnSignature() {
		TestBean target = new TestBean();
		UnsupportedOperationException expectedException = new UnsupportedOperationException();
		List<Advisor> advisors = getFixture().getAdvisors(
				new SingletonMetadataAwareAspectInstanceFactory(new ExceptionAspect(expectedException), "someBean"));
		assertThat(advisors.size()).as("One advice method was found").isEqualTo(1);
		ITestBean itb = (ITestBean) createProxy(target, advisors, ITestBean.class);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(
				itb::getAge);
	}

	// TODO document this behaviour.
	// Is it different AspectJ behaviour, at least for checked exceptions?
	@Test
	public void testAspectMethodThrowsExceptionIllegalOnSignature() {
		TestBean target = new TestBean();
		RemoteException expectedException = new RemoteException();
		List<Advisor> advisors = getFixture().getAdvisors(
				new SingletonMetadataAwareAspectInstanceFactory(new ExceptionAspect(expectedException), "someBean"));
		assertThat(advisors.size()).as("One advice method was found").isEqualTo(1);
		ITestBean itb = (ITestBean) createProxy(target, advisors, ITestBean.class);
		assertThatExceptionOfType(UndeclaredThrowableException.class).isThrownBy(
				itb::getAge).withCause(expectedException);
	}

	protected Object createProxy(Object target, List<Advisor> advisors, Class<?>... interfaces) {
		ProxyFactory pf = new ProxyFactory(target);
		if (interfaces.length > 1 || interfaces[0].isInterface()) {
			pf.setInterfaces(interfaces);
		}
		else {
			pf.setProxyTargetClass(true);
		}

		// Required everywhere we use AspectJ proxies
		pf.addAdvice(ExposeInvocationInterceptor.INSTANCE);
		pf.addAdvisors(advisors);

		pf.setExposeProxy(true);
		return pf.getProxy();
	}

	@Test
	public void testTwoAdvicesOnOneAspect() {
		TestBean target = new TestBean();
		TwoAdviceAspect twoAdviceAspect = new TwoAdviceAspect();
		List<Advisor> advisors = getFixture().getAdvisors(
				new SingletonMetadataAwareAspectInstanceFactory(twoAdviceAspect, "someBean"));
		assertThat(advisors.size()).as("Two advice methods found").isEqualTo(2);
		ITestBean itb = (ITestBean) createProxy(target, advisors, ITestBean.class);
		itb.setName("");
		assertThat(itb.getAge()).isEqualTo(0);
		int newAge = 32;
		itb.setAge(newAge);
		assertThat(itb.getAge()).isEqualTo(1);
	}

	@Test
	public void testAfterAdviceTypes() throws Exception {
		Echo target = new Echo();
		ExceptionHandling afterReturningAspect = new ExceptionHandling();
		List<Advisor> advisors = getFixture().getAdvisors(
				new SingletonMetadataAwareAspectInstanceFactory(afterReturningAspect, "someBean"));
		Echo echo = (Echo) createProxy(target, advisors, Echo.class);
		assertThat(afterReturningAspect.successCount).isEqualTo(0);
		assertThat(echo.echo("")).isEqualTo("");
		assertThat(afterReturningAspect.successCount).isEqualTo(1);
		assertThat(afterReturningAspect.failureCount).isEqualTo(0);
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() ->
			echo.echo(new FileNotFoundException()));
		assertThat(afterReturningAspect.successCount).isEqualTo(1);
		assertThat(afterReturningAspect.failureCount).isEqualTo(1);
		assertThat(afterReturningAspect.afterCount).isEqualTo(afterReturningAspect.failureCount + afterReturningAspect.successCount);
	}

	@Test
	public void testFailureWithoutExplicitDeclarePrecedence() {
		TestBean target = new TestBean();
		MetadataAwareAspectInstanceFactory aspectInstanceFactory = new SingletonMetadataAwareAspectInstanceFactory(
				new NoDeclarePrecedenceShouldFail(), "someBean");
		ITestBean itb = (ITestBean) createProxy(target,
				getFixture().getAdvisors(aspectInstanceFactory), ITestBean.class);
		itb.getAge();
	}

	@Test
	public void testDeclarePrecedenceNotSupported() {
		TestBean target = new TestBean();
		assertThatIllegalArgumentException().isThrownBy(() -> {
				MetadataAwareAspectInstanceFactory aspectInstanceFactory = new SingletonMetadataAwareAspectInstanceFactory(
							new DeclarePrecedenceShouldSucceed(), "someBean");
				createProxy(target, getFixture().getAdvisors(aspectInstanceFactory), ITestBean.class);
		});
	}


	@Aspect("percflow(execution(* *(..)))")
	public static class PerCflowAspect {
	}


	@Aspect("percflowbelow(execution(* *(..)))")
	public static class PerCflowBelowAspect {
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


	@Aspect("pertypewithin(org.springframework.tests.sample.beans.IOther+)")
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

		@Override
		public Object getAspectInstance() {
			++this.count;
			return new PerTypeWithinAspect();
		}

		@Override
		public ClassLoader getAspectClassLoader() {
			return PerTypeWithinAspect.class.getClassLoader();
		}

		@Override
		public AspectMetadata getAspectMetadata() {
			return new AspectMetadata(PerTypeWithinAspect.class, "perTypeWithin");
		}

		@Override
		public Object getAspectCreationMutex() {
			return this;
		}

		@Override
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
		// @ArgNames({"age"})	// AMC needs more work here? ignoring pjp arg... ok??
		// argNames should be supported in Around as it is in Pointcut
		public void changeReturnType(ProceedingJoinPoint pjp, int age) throws Throwable {
			pjp.proceed(new Object[] {age*2});
		}
	}


	@Aspect
	public static class ManyValuedArgs {

		public String mungeArgs(String a, int b, int c, String d, StringBuffer e) {
			return a + b + c + d + e;
		}

		@Around(value="execution(String mungeArgs(..)) && args(a, b, c, d, e)", argNames="b,c,d,e,a")
		public String reverseAdvice(ProceedingJoinPoint pjp, int b, int c, String d, StringBuffer e, String a) throws Throwable {
			assertThat(pjp.proceed()).isEqualTo(a + b+ c+ d+ e);
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
	@DeclarePrecedence("test..*")
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


/**
 * Add a DeclareParents field in concrete subclasses, to identify
 * the type pattern to apply the introduction to.
 *
 * @author Rod Johnson
 * @since 2.0
 */
@Aspect
abstract class AbstractMakeModifiable {

	public interface MutableModifiable extends Modifiable {

		void markDirty();
	}

	public static class ModifiableImpl implements MutableModifiable {

		private boolean modified;

		@Override
		public void acceptChanges() {
			modified = false;
		}

		@Override
		public boolean isModified() {
			return modified;
		}

		@Override
		public void markDirty() {
			this.modified = true;
		}
	}

	@Before(value="execution(void set*(*)) && this(modifiable) && args(newValue)", argNames="modifiable,newValue")
	public void recordModificationIfSetterArgumentDiffersFromOldValue(
			JoinPoint jp, MutableModifiable mixin, Object newValue) {

		/*
		 * We use the mixin to check and, if necessary, change,
		 * modification status. We need the JoinPoint to get the
		 * setter method. We use newValue for comparison.
		 * We try to invoke the getter if possible.
		 */

		if (mixin.isModified()) {
			// Already changed, don't need to change again
			//System.out.println("changed");
			return;
		}

		// Find the current raw value, by invoking the corresponding setter
		Method correspondingGetter = getGetterFromSetter(((MethodSignature) jp.getSignature()).getMethod());
		boolean modified = true;
		if (correspondingGetter != null) {
			try {
				Object oldValue = correspondingGetter.invoke(jp.getTarget());
				//System.out.println("Old value=" + oldValue + "; new=" + newValue);
				modified = !ObjectUtils.nullSafeEquals(oldValue, newValue);
			}
			catch (Exception ex) {
				ex.printStackTrace();
				// Don't sweat on exceptions; assume value was modified
			}
		}
		else {
			//System.out.println("cannot get getter for " + jp);
		}
		if (modified) {
			mixin.markDirty();
		}
	}

	private Method getGetterFromSetter(Method setter) {
		String getterName = setter.getName().replaceFirst("set", "get");
		try {
			return setter.getDeclaringClass().getMethod(getterName);
		}
		catch (NoSuchMethodException ex) {
			// must be write only
			return null;
		}
	}

}


/**
 * Adds a declare parents pointcut.
 * @author Rod Johnson
 * @since 2.0
 */
@Aspect
class MakeITestBeanModifiable extends AbstractMakeModifiable {

	@DeclareParents(value = "org.springframework.tests.sample.beans.ITestBean+",
			defaultImpl=ModifiableImpl.class)
	public static MutableModifiable mixin;

}


/**
 * Adds a declare parents pointcut - spr5307
 * @author Andy Clement
 * @since 3.0
 */
@Aspect
class MakeAnnotatedTypeModifiable extends AbstractMakeModifiable {

	@DeclareParents(value = "(@org.springframework.aop.aspectj.annotation.Measured *)",
			defaultImpl = DefaultLockable.class)
	public static Lockable mixin;

}


/**
 * Demonstrates introductions, AspectJ annotation style.
 */
@Aspect
class MakeLockable {

	@DeclareParents(value = "org.springframework..*", defaultImpl = DefaultLockable.class)
	public static Lockable mixin;

	@Before(value="execution(void set*(*)) && this(mixin)", argNames="mixin")
	public void checkNotLocked( Lockable mixin) {
		// Can also obtain the mixin (this) this way
		//Lockable mixin = (Lockable) jp.getThis();
		if (mixin.locked()) {
			throw new IllegalStateException();
		}
	}

}


class CannotBeUnlocked implements Lockable, Comparable<Object> {

	@Override
	public void lock() {
	}

	@Override
	public void unlock() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean locked() {
		return true;
	}

	@Override
	public int compareTo(Object arg0) {
		throw new UnsupportedOperationException();
	}

}


/**
 * Used as a mixin.
 *
 * @author Rod Johnson
 */
interface Modifiable {

	boolean isModified();

	void acceptChanges();

}


/**
 * Used as a target.
 * @author Andy Clement
 */
interface AnnotatedTarget {
}


@Measured
class AnnotatedTargetImpl implements AnnotatedTarget {
}


@Retention(RetentionPolicy.RUNTIME)
@interface Measured {}

class NotLockable {

	private int intValue;

	public int getIntValue() {
		return intValue;
	}

	public void setIntValue(int intValue) {
		this.intValue = intValue;
	}

}


@Aspect("perthis(execution(* *.getSpouse()))")
class PerThisAspect {

	public int count;

	// Just to check that this doesn't cause problems with introduction processing
	@SuppressWarnings("unused")
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
