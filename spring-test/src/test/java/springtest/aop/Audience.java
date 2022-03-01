package springtest.aop;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 * @author shangzhidong@zhuanzhuan.com
 * @date 2018-07-05 21:08
 */
//@Component
@Aspect
public class Audience {

    @Before("execution(* org.adamx.springtest.aop.Performance.perform(..))")
    public void silenceCellPhones() {
        System.out.println("Silencing cell phones");
    }

    @Before("execution(* org.adamx.springtest.aop.Performance.perform(..))")
    public void takeSeats() {
        System.out.println("Taking seats");
    }

    @AfterReturning("execution(* org.adamx.springtest.aop.Performance.perform(..))")
    public void applause() {
        System.out.println("CLAP CLAP CLAP !!!");
    }

    @AfterThrowing("execution(* org.adamx.springtest.aop.Performance.perform(..))")
    public void demandRefund() {
        System.out.println("Demanding a refund");
    }
}
