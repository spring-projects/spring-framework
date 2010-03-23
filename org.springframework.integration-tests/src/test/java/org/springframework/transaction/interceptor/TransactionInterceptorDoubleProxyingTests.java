/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.transaction.interceptor;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


/**
 * Tests cornering SPR-7009.
 *
 * @author Chris Beams
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class TransactionInterceptorDoubleProxyingTests {

	@Autowired
	TestRepository repository;

	@Test
	public void test1() {
		// method 1 is required, so no problem
		assertThat(repository.method1(), equalTo("result1"));
	}

	@Test(expected = IllegalTransactionStateException.class)
	public void test2() {
		// method 2 is mandatory, so expect exception
		assertThat(repository.method2(), equalTo("result2"));
	}

}


interface TestRepository {

	public String method1();

	public String method2();

}

@Repository("testRepository")
class TestRepositoryImpl implements TestRepository {

	@Transactional
	public String method1() {
		return "result1";
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public String method2() {
		return "result2";
	}

}
