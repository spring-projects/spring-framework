package lc.org.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.core.annotation.Order;

/**
 * @author : liuc
 * @date : 2019/7/8 16:09
 * @description :
 */
@Aspect
@Order(2)
public class AspectJTest1th {

	@Pointcut("execution(* lc.org.bean.*.test*())")
	public void test(){

	}

	@Around("test()")
	public Object aroundTest(ProceedingJoinPoint p){
		System.out.println("around before....");
		Object o = null;
		try {
			o = p.proceed();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		System.out.println("around after....");
		return o;
	}

	@Before("test()")
	public void beforeTest(){
		System.out.println("before.....");
	}

	@After("test()")
	public void afterTest(){
		System.out.println("after.....");
	}


}
