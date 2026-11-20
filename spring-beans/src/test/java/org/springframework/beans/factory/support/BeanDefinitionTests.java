/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.support;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
class BeanDefinitionTests {

	@Test
	void beanDefinitionEquality() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setAbstract(true);
		bd.setLazyInit(true);
		bd.setScope("request");
		RootBeanDefinition otherBd = new RootBeanDefinition(TestBean.class);
		assertThat(bd).isNotEqualTo(otherBd);
		assertThat(otherBd).isNotEqualTo(bd);
		otherBd.setAbstract(true);
		otherBd.setLazyInit(true);
		otherBd.setScope("request");
		assertThat(bd).isEqualTo(otherBd);
		assertThat(otherBd).isEqualTo(bd);
		assertThat(bd.hashCode()).isEqualTo(otherBd.hashCode());
	}

	@Test
	void beanDefinitionEqualityWithPropertyValues() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.getPropertyValues().add("name", "myName");
		bd.getPropertyValues().add("age", "99");
		RootBeanDefinition otherBd = new RootBeanDefinition(TestBean.class);
		otherBd.getPropertyValues().add("name", "myName");
		assertThat(bd).isNotEqualTo(otherBd);
		assertThat(otherBd).isNotEqualTo(bd);
		otherBd.getPropertyValues().add("age", "11");
		assertThat(bd).isNotEqualTo(otherBd);
		assertThat(otherBd).isNotEqualTo(bd);
		otherBd.getPropertyValues().add("age", "99");
		assertThat(bd).isEqualTo(otherBd);
		assertThat(otherBd).isEqualTo(bd);
		assertThat(bd.hashCode()).isEqualTo(otherBd.hashCode());
	}

	@Test
	void beanDefinitionEqualityWithConstructorArguments() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue("test");
		bd.getConstructorArgumentValues().addIndexedArgumentValue(1, 5);
		RootBeanDefinition otherBd = new RootBeanDefinition(TestBean.class);
		otherBd.getConstructorArgumentValues().addGenericArgumentValue("test");
		assertThat(bd).isNotEqualTo(otherBd);
		assertThat(otherBd).isNotEqualTo(bd);
		otherBd.getConstructorArgumentValues().addIndexedArgumentValue(1, 9);
		assertThat(bd).isNotEqualTo(otherBd);
		assertThat(otherBd).isNotEqualTo(bd);
		otherBd.getConstructorArgumentValues().addIndexedArgumentValue(1, 5);
		assertThat(bd).isEqualTo(otherBd);
		assertThat(otherBd).isEqualTo(bd);
		assertThat(bd.hashCode()).isEqualTo(otherBd.hashCode());
	}

	@Test
	void beanDefinitionEqualityWithTypedConstructorArguments() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue("test", "int");
		bd.getConstructorArgumentValues().addIndexedArgumentValue(1, 5, "long");
		RootBeanDefinition otherBd = new RootBeanDefinition(TestBean.class);
		otherBd.getConstructorArgumentValues().addGenericArgumentValue("test", "int");
		otherBd.getConstructorArgumentValues().addIndexedArgumentValue(1, 5);
		assertThat(bd).isNotEqualTo(otherBd);
		assertThat(otherBd).isNotEqualTo(bd);
		otherBd.getConstructorArgumentValues().addIndexedArgumentValue(1, 5, "int");
		assertThat(bd).isNotEqualTo(otherBd);
		assertThat(otherBd).isNotEqualTo(bd);
		otherBd.getConstructorArgumentValues().addIndexedArgumentValue(1, 5, "long");
		assertThat(bd).isEqualTo(otherBd);
		assertThat(otherBd).isEqualTo(bd);
		assertThat(bd.hashCode()).isEqualTo(otherBd.hashCode());
	}

	@Test
	void genericBeanDefinitionEquality() {
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setParentName("parent");
		bd.setScope("request");
		bd.setAbstract(true);
		bd.setLazyInit(true);
		GenericBeanDefinition otherBd = new GenericBeanDefinition();
		otherBd.setScope("request");
		otherBd.setAbstract(true);
		otherBd.setLazyInit(true);
		assertThat(bd).isNotEqualTo(otherBd);
		assertThat(otherBd).isNotEqualTo(bd);
		otherBd.setParentName("parent");
		assertThat(bd).isEqualTo(otherBd);
		assertThat(otherBd).isEqualTo(bd);
		assertThat(bd.hashCode()).isEqualTo(otherBd.hashCode());

		bd.getPropertyValues();
		assertThat(bd).isEqualTo(otherBd);
		assertThat(otherBd).isEqualTo(bd);
		assertThat(bd.hashCode()).isEqualTo(otherBd.hashCode());

		bd.getConstructorArgumentValues();
		assertThat(bd).isEqualTo(otherBd);
		assertThat(otherBd).isEqualTo(bd);
		assertThat(bd.hashCode()).isEqualTo(otherBd.hashCode());
	}

	@Test
	void beanDefinitionHolderEquality() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setAbstract(true);
		bd.setLazyInit(true);
		bd.setScope("request");
		BeanDefinitionHolder holder = new BeanDefinitionHolder(bd, "bd");
		RootBeanDefinition otherBd = new RootBeanDefinition(TestBean.class);
		assertThat(bd).isNotEqualTo(otherBd);
		assertThat(otherBd).isNotEqualTo(bd);
		otherBd.setAbstract(true);
		otherBd.setLazyInit(true);
		otherBd.setScope("request");
		BeanDefinitionHolder otherHolder = new BeanDefinitionHolder(bd, "bd");
		assertThat(holder).isEqualTo(otherHolder);
		assertThat(otherHolder).isEqualTo(holder);
		assertThat(holder.hashCode()).isEqualTo(otherHolder.hashCode());
	}

	@Test
	void beanDefinitionMerging() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue("test");
		bd.getConstructorArgumentValues().addIndexedArgumentValue(1, 5);
		bd.getPropertyValues().add("name", "myName");
		bd.getPropertyValues().add("age", "99");
		bd.setQualifiedElement(getClass());

		GenericBeanDefinition childBd = new GenericBeanDefinition();
		childBd.setParentName("bd");

		RootBeanDefinition mergedBd = new RootBeanDefinition(bd);
		mergedBd.overrideFrom(childBd);
		assertThat(mergedBd.getConstructorArgumentValues().getArgumentCount()).isEqualTo(2);
		assertThat(mergedBd.getPropertyValues()).hasSize(2);
		assertThat(mergedBd).isEqualTo(bd);

		mergedBd.getConstructorArgumentValues().getArgumentValue(1, null).setValue(9);
		assertThat(bd.getConstructorArgumentValues().getArgumentValue(1, null).getValue()).isEqualTo(5);
		assertThat(bd.getQualifiedElement()).isEqualTo(getClass());
	}

}
