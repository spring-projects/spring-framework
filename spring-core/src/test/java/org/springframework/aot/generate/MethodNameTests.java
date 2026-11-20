/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.generate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link MethodName}.
 *
 * @author Phillip Webb
 */
class MethodNameTests {

	@Test
	void ofWhenPartsIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> MethodName.of((String[]) null))
				.withMessage("'parts' must not be null");
	}

	@Test
	void ofReturnsMethodName() {
		assertThat(MethodName.of("get", "bean", "factory")).hasToString("getBeanFactory");
		assertThat(MethodName.of("get", null, "factory")).hasToString("getFactory");
		assertThat(MethodName.of(null, null)).hasToString("$$aot");
		assertThat(MethodName.of("", null)).hasToString("$$aot");
		assertThat(MethodName.of("get", "InputStream")).hasToString("getInputStream");
		assertThat(MethodName.of("register", "myBean123", "bean")).hasToString("registerMyBeanBean");
		assertThat(MethodName.of("register", "org.springframework.example.bean"))
				.hasToString("registerOrgSpringframeworkExampleBean");
	}

	@Test
	void andPartsWhenPartsIsNullThrowsException() {
		MethodName name = MethodName.of("myBean");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> name.and(((String[]) null)))
				.withMessage("'parts' must not be null");
	}

	@Test
	void andPartsReturnsMethodName() {
		MethodName name = MethodName.of("myBean");
		assertThat(name.and("test")).hasToString("myBeanTest");
		assertThat(name.and("test", null)).hasToString("myBeanTest");
		assertThat(name.and("getName")).hasToString("getMyBeanName");
		assertThat(name.and("setName")).hasToString("setMyBeanName");
		assertThat(name.and("isDoingOk")).hasToString("isMyBeanDoingOk");
		assertThat(name.and("this", "that", "the", "other")).hasToString("myBeanThisThatTheOther");
	}

	@Test
	void andNameWhenPartsIsNullThrowsException() {
		MethodName name = MethodName.of("myBean");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> name.and(((MethodName) null)))
				.withMessage("'name' must not be null");
	}

	@Test
	void andNameReturnsMethodName() {
		MethodName name = MethodName.of("myBean");
		assertThat(name.and(MethodName.of("test"))).hasToString("myBeanTest");
	}

	@Test
	void hashCodeAndEquals() {
		MethodName name1 = MethodName.of("myBean");
		MethodName name2 = MethodName.of("my", "bean");
		MethodName name3 = MethodName.of("myOtherBean");
		assertThat(name1.hashCode()).isEqualTo(name2.hashCode());
		assertThat(name1).isEqualTo(name1).isEqualTo(name2).isNotEqualTo(name3);
	}

}
