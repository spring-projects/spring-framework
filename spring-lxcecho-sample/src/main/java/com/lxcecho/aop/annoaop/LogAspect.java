package com.lxcecho.aop.annoaop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 切面类
 * 执行顺序：
 * 1，@Around 环绕通知
 * 2. @Before 前置通知
 * 3. 调用 方法
 * 4. @Around 方法执行耗时
 * 5. @After 后置通知
 * 6. @AfterReturning 返回通知
 * 7. @AfterThrowing 异常通知
 *
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/10
 */
@Aspect // 切面类
@Component // ioc 容器
public class LogAspect {

	/**
	 * 设置切入点和通知类型：
	 * 	切入点表达式: execution(访问修饰符 增强方法返回类型 增强方法所在类全路径.方法名称(方法参数))
	 * 	execution：用于匹配方法执行的连接点
	 *
	 * 定义一个切入点，后面的通知直接引入切入点方法 pointCut() 即可
	 * 第一个 *：表示返回值任意类型
	 * 第二个 .*(..)：表示任何方法名，括号表示参数，两个点 .. 表示任何参数类型
	 */
	@Pointcut(value = "execution(* com.lxcecho.aop.annoaop.CalculatorImpl.*(..))")
	public void pointCut() {
	}

	/**
	 * 前置 @Before(value="切入点表达式配置切入点")：进入环绕后执行，下一步执行方法
	 *
	 * @param joinPoint
	 */
	@Before(value = "pointCut()")
//	@Before("execution(* com.lxcecho.aop.annoaop.CalculatorImpl.*(..))")
	public void beforeMethod(JoinPoint joinPoint) {
		String methodName = joinPoint.getSignature().getName();
		Object[] args = joinPoint.getArgs();
		System.out.println("Logger-->前置通知，方法名称：" + methodName + "，参数：" + Arrays.toString(args));
	}

	/**
	 * 后置 @After()：返回之前执行
	 *
	 * @param joinPoint
	 */
	@After(value = "pointCut()")
//	@After(value = "execution(* com.lxcecho.aop.annoaop.CalculatorImpl.*(..))")
	public void afterMethod(JoinPoint joinPoint) {
		String methodName = joinPoint.getSignature().getName();
		System.out.println("Logger-->后置通知，方法名称：" + methodName);
	}

	/**
	 * 返回 @AfterReturning：正常返回执行，最后执行
	 *
	 * @param joinPoint
	 * @param result
	 */
	@AfterReturning(value = "pointCut()", returning = "result")
//	@AfterReturning(value = "execution(* com.lxcecho.aop.annoaop.CalculatorImpl.*(..))", returning = "result")
	public void afterReturningMethod(JoinPoint joinPoint, Object result) {
		String methodName = joinPoint.getSignature().getName();
		System.out.println("Logger-->返回通知，方法名称：" + methodName + "，返回结果：" + result);
	}

	/**
	 * 异常 @AfterThrowing 获取到目标方法异常信息：目标方法出现异常，这个通知执行
	 *
	 * @param joinPoint
	 * @param ex
	 */
	@AfterThrowing(value = "pointCut()", throwing = "ex")
//	@AfterThrowing(value = "execution(* com.lxcecho.aop.annoaop.CalculatorImpl.*(..))", throwing = "ex")
	public void afterThrowingMethod(JoinPoint joinPoint, Throwable ex) {
		String methodName = joinPoint.getSignature().getName();
		System.out.println("Logger-->异常通知，方法名称：" + methodName + "，异常信息：" + ex);
	}

	/**
	 * 环绕 @Around()：连接到切入点开始执行，下一步进入前置通知，再下一步才是执行操作方法
	 *
	 * @param joinPoint
	 * @return
	 */
	/*@Around("pointCut()")
	@Around("execution(* com.lxcecho.aop.annoaop.CalculatorImpl.*(..))")
	public Object aroundMethod(ProceedingJoinPoint joinPoint) {
		String methodName = joinPoint.getSignature().getName();
		Object[] args = joinPoint.getArgs();
		String argString = Arrays.toString(args);
		Object result = null;
		try {
			System.out.println("环绕通知==目标方法之前执行");
			// 调用目标方法
			result = joinPoint.proceed();
			System.out.println("环绕通知==目标方法返回值之后");
		} catch (Throwable throwable) {
			throwable.printStackTrace();
			System.out.println("环绕通知==目标方法出现异常执行");
		} finally {
			System.out.println("环绕通知==目标方法执行完毕执行");
		}
		return result;
	}*/

}
