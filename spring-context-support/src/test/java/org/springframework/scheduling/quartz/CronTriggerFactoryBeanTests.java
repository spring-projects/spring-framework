/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.quartz;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.quartz.Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY;
import static org.quartz.Trigger.MISFIRE_INSTRUCTION_SMART_POLICY;

/**
 * Tests for {@link CronTriggerFactoryBean}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class CronTriggerFactoryBeanTests {

	private final CronTriggerFactoryBean factory = new CronTriggerFactoryBean();


	@Test
	void createWithoutJobDetail() throws ParseException {
		factory.setName("myTrigger");
		factory.setCronExpression("0 15 10 ? * *");
		factory.afterPropertiesSet();
		CronTrigger trigger = factory.getObject();
		assertThat(trigger.getCronExpression()).isEqualTo("0 15 10 ? * *");
	}

	@Test
	void setMisfireInstructionNameToUnsupportedValues() {
		assertThatIllegalArgumentException().isThrownBy(() -> factory.setMisfireInstructionName(null));
		assertThatIllegalArgumentException().isThrownBy(() -> factory.setMisfireInstructionName("   "));
		assertThatIllegalArgumentException().isThrownBy(() -> factory.setMisfireInstructionName("bogus"));
	}

	/**
	 * This test effectively verifies that the internal 'constants' map is properly
	 * configured for all MISFIRE_INSTRUCTION_ constants defined in {@link CronTrigger}.
	 */
	@Test
	void setMisfireInstructionNameToAllSupportedValues() {
		streamMisfireInstructionConstants()
				.map(Field::getName)
				.forEach(name -> assertThatNoException().as(name).isThrownBy(() -> factory.setMisfireInstructionName(name)));
	}

	@Test
	void setMisfireInstruction() {
		assertThatIllegalArgumentException().isThrownBy(() -> factory.setMisfireInstruction(999));

		assertThatNoException().isThrownBy(() -> factory.setMisfireInstruction(MISFIRE_INSTRUCTION_SMART_POLICY));
		assertThatNoException().isThrownBy(() -> factory.setMisfireInstruction(MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY));
		assertThatNoException().isThrownBy(() -> factory.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW));
		assertThatNoException().isThrownBy(() -> factory.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING));
	}


	private static Stream<Field> streamMisfireInstructionConstants() {
		return Arrays.stream(CronTrigger.class.getFields())
				.filter(ReflectionUtils::isPublicStaticFinal)
				.filter(field -> field.getName().startsWith("MISFIRE_INSTRUCTION_"));
	}

}
