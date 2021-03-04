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

package org.springframework.aop.aspectj.autoproxy.spr3064;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Adrian Colyer
 * @author Chris Beams
 */
public class SPR3064Tests {

	private Service service;


	@Test
	public void testServiceIsAdvised() {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());

		service = (Service) ctx.getBean("service");
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(
				this.service::serveMe)
			.withMessageContaining("advice invoked");
	}

}


@Retention(RetentionPolicy.RUNTIME)
@interface Transaction {
}


@Aspect
class TransactionInterceptor {

	@Around(value="execution(* *..Service.*(..)) && @annotation(transaction)")
	public Object around(ProceedingJoinPoint pjp, Transaction transaction) throws Throwable {
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
