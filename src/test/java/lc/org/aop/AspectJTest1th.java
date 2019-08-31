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

	@DeclareParents(value = "lc.org.beans.TestBean1th",defaultImpl = AopBean.class)
	public IAopBean aopBean;

	@Pointcut(value = "execution(* lc.org.beans.*.test())")
	public void test(){
		System.out.println("point cut");
	}

	@Before("test()")
	public void beforeTest(){
		System.out.println("before.....");
	}

	@Before("test()")
	@After("test()")
	public void afterTest(){
		System.out.println("after.....");
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
}
