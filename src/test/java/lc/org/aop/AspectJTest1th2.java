package lc.org.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

/**
 * @author : liuc
 * @date : 2019/7/8 16:09
 * @description :
 */
@Aspect
//@Order(1)
public class AspectJTest1th2 {

	@Pointcut("execution(* lc.org.beans.*.test())")
	public void test(){

	}

	public void noPointcut(){

	}
	//@Around("noPointcut()") //报错，解析pointcut时找不到pointcut引用，将无法创建bean
	//@Around("test()")
	@Around("lc.org.aop.AspectJTest1th.test()")
	public Object aroundTest(ProceedingJoinPoint p){
		System.out.println("around before 2....");
		Object o = null;
		try {
			o = p.proceed();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		System.out.println("around after 2....");
		return o;
	}

	@Before("test()")
	public void beforeTest(){
		System.out.println("before 2.....");
	}

	@After("test()")
	public void afterTest(){
		System.out.println("after 2.....");
	}


}
