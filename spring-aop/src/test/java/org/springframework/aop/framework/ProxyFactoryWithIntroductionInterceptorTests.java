package java.org.springframework.aop.framework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DelegatePerTargetObjectIntroductionInterceptor;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;

/**
 * @author 占道宏
 * @create 2023/10/10 9:59
 */
public class ProxyFactoryWithIntroductionInterceptorTests {

    /**
     * The target object does not implement any interfaces, and in this case, you want to use CGLIB for dynamic proxying.
     */
    @Test
    public void testDelegatingIntroductionInterceptorWithoutInterface() {
        People peo = new People();
        ProxyFactory pf = new ProxyFactory();
        DelegatingIntroductionInterceptor dii = new DelegatingIntroductionInterceptor((Developer) () -> System.out.println("Coding"));
        pf.addAdvice(dii);
        pf.setTarget(peo);

        Object proxy = pf.getProxy();
        Assertions.assertTrue(AopUtils.isCglibProxy(proxy));
        Assertions.assertTrue(proxy instanceof People);
        Assertions.assertTrue(proxy instanceof Developer);

        People people = (People) proxy;
        Assertions.assertDoesNotThrow(people::eat);

        Developer developer = (Developer) proxy;
        Assertions.assertDoesNotThrow(developer::code);
    }

    /**
     * The target object implements the Teacher interface, and in this case, you want to use JDK for dynamic proxying
     */
    @Test
    public void testDelegatingIntroductionInterceptorWithInterface() {
        Teacher teacher = () -> System.out.println("teach");
        ProxyFactory pf = new ProxyFactory();
        DelegatingIntroductionInterceptor dii = new DelegatingIntroductionInterceptor((Developer) () -> System.out.println("Coding"));
        pf.addAdvice(dii);
        pf.addInterface(Teacher.class);
        pf.setTarget(teacher);

        Object proxy = pf.getProxy();
        Assertions.assertTrue(AopUtils.isJdkDynamicProxy(proxy));
        Assertions.assertTrue(proxy instanceof Teacher);
        Assertions.assertTrue(proxy instanceof Developer);

        Teacher teacher1 = (Teacher) proxy;
        Assertions.assertDoesNotThrow(teacher1::teach);

        Developer developer = (Developer) proxy;
        Assertions.assertDoesNotThrow(developer::code);
    }

    /**
     * The target object does not implement any interfaces, and in this case, you want to use CGLIB for dynamic proxying.
     */
    @Test
    public void testDelegatePerTargetObjectIntroductionInterceptorWithoutInterface() {
        People peo = new People();
        ProxyFactory pf = new ProxyFactory();
        DelegatePerTargetObjectIntroductionInterceptor dii = new DelegatePerTargetObjectIntroductionInterceptor(DeveloperImpl.class, Developer.class);
        pf.addAdvice(dii);
        pf.setTarget(peo);

        Object proxy = pf.getProxy();
        Assertions.assertTrue(AopUtils.isCglibProxy(proxy));
        Assertions.assertTrue(proxy instanceof People);
        Assertions.assertTrue(proxy instanceof Developer);

        People people = (People) proxy;
        Assertions.assertDoesNotThrow(people::eat);

        Developer developer = (Developer) proxy;
        Assertions.assertDoesNotThrow(developer::code);
    }

    /**
     * The target object implements the Teacher interface, and in this case, you want to use JDK for dynamic proxying
     */
    @Test
    public void testDelegatePerTargetObjectIntroductionInterceptorWithInterface() {
        Teacher teacher = () -> System.out.println("teach");
        ProxyFactory pf = new ProxyFactory();
        DelegatePerTargetObjectIntroductionInterceptor dii = new DelegatePerTargetObjectIntroductionInterceptor(DeveloperImpl.class, Developer.class);
        pf.addAdvice(dii);
        pf.addInterface(Teacher.class);
        pf.setTarget(teacher);

        Object proxy = pf.getProxy();
        Assertions.assertTrue(AopUtils.isJdkDynamicProxy(proxy));
        Assertions.assertTrue(proxy instanceof Teacher);
        Assertions.assertTrue(proxy instanceof Developer);

        Teacher teacher1 = (Teacher) proxy;
        Assertions.assertDoesNotThrow(teacher1::teach);

        Developer developer = (Developer) proxy;
        Assertions.assertDoesNotThrow(developer::code);
    }

    /**
     * The target object does not implement any interfaces, so it is necessary to use CGLIB for proxying
     */
    @Test
    public void testProxyFactoryWithoutInterface() {
        People people = new People();
        ProxyFactory pf = new ProxyFactory();
        pf.setTarget(people);
        Object proxy = pf.getProxy();

        Assertions.assertTrue(AopUtils.isCglibProxy(proxy));
        Assertions.assertTrue(proxy instanceof People);
        Assertions.assertDoesNotThrow(((People)proxy)::eat);

        pf.addInterface(Teacher.class);
        proxy = pf.getProxy();
        Assertions.assertTrue(AopUtils.isCglibProxy(proxy));
        Assertions.assertTrue(proxy instanceof Teacher);
        Assertions.assertTrue(proxy instanceof People);
        Assertions.assertDoesNotThrow(((People)proxy)::eat);
    }

    /**
     * When the target object implements the Teacher interface
     * but we have not explicitly called the addInterface method,
     * we expect to use CGLIB; however, after calling it, we expect to use JDK
     */
    @Test
    public void testProxyFactoryWithInterface() {
        Teacher teacher = () -> System.out.println("teach");
        ProxyFactory pf = new ProxyFactory();
        pf.setTarget(teacher);
        Object proxy = pf.getProxy();

        Assertions.assertTrue(AopUtils.isCglibProxy(proxy));
        Assertions.assertTrue(proxy instanceof Teacher);
        Assertions.assertDoesNotThrow(((Teacher)proxy)::teach);

        pf.addInterface(Teacher.class);
        proxy = pf.getProxy();
        Assertions.assertTrue(AopUtils.isJdkDynamicProxy(proxy));
        Assertions.assertTrue(proxy instanceof Teacher);
        Assertions.assertDoesNotThrow(((Teacher)proxy)::teach);
    }

    public static class People {
        void eat() {
            System.out.println("eat");
        }
    }

    public interface Teacher {
        void teach();
    }

    public interface Developer {
        void code();
    }

    public static class DeveloperImpl implements Developer {
        @Override
        public void code() {
            System.out.println("Coding");
        }
    }
}
