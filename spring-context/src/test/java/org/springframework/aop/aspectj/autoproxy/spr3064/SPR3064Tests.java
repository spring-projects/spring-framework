/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj.autoproxy.spr3064;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThatRuntimeException;

/**
 * @author Adrian Colyer
 * @author Chris Beams
 */
class SPR3064Tests {

	@Test
	void serviceIsAdvised() {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());

		Service service = ctx.getBean(Service.class);
		assertThatRuntimeException()
			.isThrownBy(service::serveMe)
			.withMessage("advice invoked");

		ctx.close();
	}

}


@Retention(RetentionPolicy.RUNTIME)
@interface Transaction {
}


@Aspect
class TransactionInterceptor {

	@Around(value="execution(* *..Service.*(..)) && @annotation(transaction)")
	public Object around(ProceedingJoinPoint pjp, Transaction transaction) {
		throw new RuntimeException("advice invoked");
		//return pjp.proceed();
	}
}


interface Service {

	void serveMe();
}


class ServiceImpl implements Service {

	@Override
	@Transaction
	public void serveMe() {
	}
}
