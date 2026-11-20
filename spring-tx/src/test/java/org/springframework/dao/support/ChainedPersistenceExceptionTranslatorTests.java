/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao.support;

import org.junit.jupiter.api.Test;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.OptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @since 2.0
 */
class ChainedPersistenceExceptionTranslatorTests {

	@Test
	void empty() {
		ChainedPersistenceExceptionTranslator pet = new ChainedPersistenceExceptionTranslator();
		RuntimeException in = new RuntimeException("in");
		assertThat(DataAccessUtils.translateIfNecessary(in, pet)).isSameAs(in);
	}

	@Test
	void exceptionTranslationWithTranslation() {
		MapPersistenceExceptionTranslator mpet1 = new MapPersistenceExceptionTranslator();
		RuntimeException in1 = new RuntimeException("in");
		InvalidDataAccessApiUsageException out1 = new InvalidDataAccessApiUsageException("out");
		InvalidDataAccessApiUsageException out2 = new InvalidDataAccessApiUsageException("out");
		mpet1.addTranslation(in1, out1);

		ChainedPersistenceExceptionTranslator chainedPet1 = new ChainedPersistenceExceptionTranslator();
		assertThat(DataAccessUtils.translateIfNecessary(in1, chainedPet1)).as("Should not translate yet").isSameAs(in1);
		chainedPet1.addDelegate(mpet1);
		assertThat(DataAccessUtils.translateIfNecessary(in1, chainedPet1)).as("Should now translate").isSameAs(out1);

		// Now add a new translator and verify it wins
		MapPersistenceExceptionTranslator mpet2 = new MapPersistenceExceptionTranslator();
		mpet2.addTranslation(in1, out2);
		chainedPet1.addDelegate(mpet2);
		assertThat(DataAccessUtils.translateIfNecessary(in1, chainedPet1)).as("Should still translate the same due to ordering").isSameAs(out1);

		ChainedPersistenceExceptionTranslator chainedPet2 = new ChainedPersistenceExceptionTranslator();
		chainedPet2.addDelegate(mpet2);
		chainedPet2.addDelegate(mpet1);
		assertThat(DataAccessUtils.translateIfNecessary(in1, chainedPet2)).as("Should translate differently due to ordering").isSameAs(out2);

		RuntimeException in2 = new RuntimeException("in2");
		OptimisticLockingFailureException out3 = new OptimisticLockingFailureException("out2");
		assertThat(chainedPet2.translateExceptionIfPossible(in2)).isNull();
		MapPersistenceExceptionTranslator mpet3 = new MapPersistenceExceptionTranslator();
		mpet3.addTranslation(in2, out3);
		chainedPet2.addDelegate(mpet3);
		assertThat(chainedPet2.translateExceptionIfPossible(in2)).isSameAs(out3);
	}

}
