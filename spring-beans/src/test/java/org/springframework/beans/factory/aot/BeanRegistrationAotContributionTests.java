/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.aot;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.testfixture.beans.factory.aot.MockBeanRegistrationCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link BeanRegistrationAotContribution}.
 *
 * @author Stephane Nicoll
 */
class BeanRegistrationAotContributionTests {

	@Test
	void concatWithBothNullReturnsNull() {
		assertThat(BeanRegistrationAotContribution.concat(null, null)).isNull();
	}

	@Test
	void concatWithFirstNullReturnsSecondAsIs() {
		BeanRegistrationAotContribution contribution = mock(BeanRegistrationAotContribution.class);
		assertThat(BeanRegistrationAotContribution.concat(null, contribution)).isSameAs(contribution);
		verifyNoInteractions(contribution);
	}

	@Test
	void concatWithSecondNullReturnsFirstAsIs() {
		BeanRegistrationAotContribution contribution = mock(BeanRegistrationAotContribution.class);
		assertThat(BeanRegistrationAotContribution.concat(contribution, null)).isSameAs(contribution);
		verifyNoInteractions(contribution);
	}

	@Test
	void concatApplyContributionsInOrder() {
		BeanRegistrationAotContribution first = mock(BeanRegistrationAotContribution.class);
		BeanRegistrationAotContribution second = mock(BeanRegistrationAotContribution.class);
		BeanRegistrationAotContribution combined = BeanRegistrationAotContribution.concat(first, second);
		assertThat(combined).isNotNull();
		TestGenerationContext generationContext = new TestGenerationContext();
		BeanRegistrationCode beanRegistrationCode = new MockBeanRegistrationCode(generationContext);
		combined.applyTo(generationContext, beanRegistrationCode);
		InOrder ordered = inOrder(first, second);
		ordered.verify(first).applyTo(generationContext, beanRegistrationCode);
		ordered.verify(second).applyTo(generationContext, beanRegistrationCode);
	}

}
