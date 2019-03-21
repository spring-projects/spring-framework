/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.dao.support;

import org.junit.Test;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.support.DataAccessUtilsTests.MapPersistenceExceptionTranslator;

import static org.junit.Assert.*;

/**
 * @author Rod Johnson
 * @since 2.0
 */
public class ChainedPersistenceExceptionTranslatorTests {

	@Test
	public void empty() {
		ChainedPersistenceExceptionTranslator pet = new ChainedPersistenceExceptionTranslator();
		//MapPersistenceExceptionTranslator mpet = new MapPersistenceExceptionTranslator();
		RuntimeException in = new RuntimeException("in");
		assertSame(in, DataAccessUtils.translateIfNecessary(in, pet));
	}

	@Test
	public void exceptionTranslationWithTranslation() {
		MapPersistenceExceptionTranslator mpet1 = new MapPersistenceExceptionTranslator();
		RuntimeException in1 = new RuntimeException("in");
		InvalidDataAccessApiUsageException out1 = new InvalidDataAccessApiUsageException("out");
		InvalidDataAccessApiUsageException out2 = new InvalidDataAccessApiUsageException("out");
		mpet1.addTranslation(in1, out1);

		ChainedPersistenceExceptionTranslator chainedPet1 = new ChainedPersistenceExceptionTranslator();
		assertSame("Should not translate yet", in1, DataAccessUtils.translateIfNecessary(in1, chainedPet1));
		chainedPet1.addDelegate(mpet1);
		assertSame("Should now translate", out1, DataAccessUtils.translateIfNecessary(in1, chainedPet1));

		// Now add a new translator and verify it wins
		MapPersistenceExceptionTranslator mpet2 = new MapPersistenceExceptionTranslator();
		mpet2.addTranslation(in1, out2);
		chainedPet1.addDelegate(mpet2);
		assertSame("Should still translate the same due to ordering",
				out1, DataAccessUtils.translateIfNecessary(in1, chainedPet1));

		ChainedPersistenceExceptionTranslator chainedPet2 = new ChainedPersistenceExceptionTranslator();
		chainedPet2.addDelegate(mpet2);
		chainedPet2.addDelegate(mpet1);
		assertSame("Should translate differently due to ordering",
				out2, DataAccessUtils.translateIfNecessary(in1, chainedPet2));

		RuntimeException in2 = new RuntimeException("in2");
		OptimisticLockingFailureException out3 = new OptimisticLockingFailureException("out2");
		assertNull(chainedPet2.translateExceptionIfPossible(in2));
		MapPersistenceExceptionTranslator mpet3 = new MapPersistenceExceptionTranslator();
		mpet3.addTranslation(in2, out3);
		chainedPet2.addDelegate(mpet3);
		assertSame(out3, chainedPet2.translateExceptionIfPossible(in2));
	}

}
