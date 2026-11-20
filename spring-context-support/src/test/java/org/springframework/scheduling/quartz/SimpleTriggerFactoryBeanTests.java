/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.quartz;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.quartz.SimpleTrigger;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.quartz.SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW;
import static org.quartz.SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT;
import static org.quartz.SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT;
import static org.quartz.SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT;
import static org.quartz.SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT;
import static org.quartz.Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY;
import static org.quartz.Trigger.MISFIRE_INSTRUCTION_SMART_POLICY;

/**
 * Tests for {@link SimpleTriggerFactoryBean}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class SimpleTriggerFactoryBeanTests {

	private final SimpleTriggerFactoryBean factory = new SimpleTriggerFactoryBean();


	@Test
	void createWithoutJobDetail() {
		factory.setName("myTrigger");
		factory.setRepeatCount(5);
		factory.setRepeatInterval(1000L);
		factory.afterPropertiesSet();
		SimpleTrigger trigger = factory.getObject();
		assertThat(trigger.getRepeatCount()).isEqualTo(5);
		assertThat(trigger.getRepeatInterval()).isEqualTo(1000L);
	}

	@Test
	void setMisfireInstructionNameToUnsupportedValues() {
		assertThatIllegalArgumentException().isThrownBy(() -> factory.setMisfireInstructionName(null));
		assertThatIllegalArgumentException().isThrownBy(() -> factory.setMisfireInstructionName("   "));
		assertThatIllegalArgumentException().isThrownBy(() -> factory.setMisfireInstructionName("bogus"));
	}

	/**
	 * This test effectively verifies that the internal 'constants' map is properly
	 * configured for all MISFIRE_INSTRUCTION_ constants defined in {@link SimpleTrigger}.
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
		assertThatNoException().isThrownBy(() -> factory.setMisfireInstruction(MISFIRE_INSTRUCTION_FIRE_NOW));
		assertThatNoException().isThrownBy(() -> factory.setMisfireInstruction(MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT));
		assertThatNoException().isThrownBy(() -> factory.setMisfireInstruction(MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT));
		assertThatNoException().isThrownBy(() -> factory.setMisfireInstruction(MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT));
		assertThatNoException().isThrownBy(() -> factory.setMisfireInstruction(MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT));
	}


	private static Stream<Field> streamMisfireInstructionConstants() {
		return Arrays.stream(SimpleTrigger.class.getFields())
				.filter(ReflectionUtils::isPublicStaticFinal)
				.filter(field -> field.getName().startsWith("MISFIRE_INSTRUCTION_"));
	}

}
