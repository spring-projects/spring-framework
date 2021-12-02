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

package org.springframework.core.annotation;

import jakarta.annotation.Priority;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
class OrderUtilsTests {

	@Test
	void getSimpleOrder() {
		assertThat(OrderUtils.getOrder(SimpleOrder.class, null)).isEqualTo(Integer.valueOf(50));
		assertThat(OrderUtils.getOrder(SimpleOrder.class, null)).isEqualTo(Integer.valueOf(50));
	}

	@Test
	void getPriorityOrder() {
		assertThat(OrderUtils.getOrder(SimplePriority.class, null)).isEqualTo(Integer.valueOf(55));
		assertThat(OrderUtils.getOrder(SimplePriority.class, null)).isEqualTo(Integer.valueOf(55));
	}

	@Test
	void getOrderWithBoth() {
		assertThat(OrderUtils.getOrder(OrderAndPriority.class, null)).isEqualTo(Integer.valueOf(50));
		assertThat(OrderUtils.getOrder(OrderAndPriority.class, null)).isEqualTo(Integer.valueOf(50));
	}

	@Test
	void getDefaultOrder() {
		assertThat(OrderUtils.getOrder(NoOrder.class, 33)).isEqualTo(33);
		assertThat(OrderUtils.getOrder(NoOrder.class, 33)).isEqualTo(33);
	}

	@Test
	void getPriorityValueNoAnnotation() {
		assertThat(OrderUtils.getPriority(SimpleOrder.class)).isNull();
		assertThat(OrderUtils.getPriority(SimpleOrder.class)).isNull();
	}

	@Test
	void getPriorityValue() {
		assertThat(OrderUtils.getPriority(OrderAndPriority.class)).isEqualTo(Integer.valueOf(55));
		assertThat(OrderUtils.getPriority(OrderAndPriority.class)).isEqualTo(Integer.valueOf(55));
	}


	@Order(50)
	private static class SimpleOrder {}

	@Priority(55)
	private static class SimplePriority {}

	@Order(50)
	@Priority(55)
	private static class OrderAndPriority {}

	private static class NoOrder {}

}
