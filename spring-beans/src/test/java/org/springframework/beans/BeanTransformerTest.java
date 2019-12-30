package org.springframework.beans;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.tests.sample.beans.ImmutableTestBean;
import org.springframework.tests.sample.beans.TestBean;

import com.hotels.transformer.model.FieldTransformer;

/**
 * Unit tests for class: {@link BeanTransformer}.
 * @author borriello.fabio
 */
public class BeanTransformerTest {
	/**
	 * Class to be tested.
	 */
	private BeanTransformer underTest;

	@BeforeEach
	public void init() {
		underTest = new BeanTransformer();
	}

	@Test
	public void testCopyPropertiesForMutableBean() throws Exception {
		TestBean testBean = createTestBean("Bob", createTestBean("Mary"));

		TestBean actual = underTest.transform(testBean, TestBean.class);

		assertThat(actual.getName().equals(testBean.getName())).as("Name copied").isTrue();
		assertThat(actual.getAge() == testBean.getAge()).as("Age copied").isTrue();
		assertThat(actual.getTouchy().equals(testBean.getTouchy())).as("Touchy copied").isTrue();
		assertThat(actual.getSpouse().getName().equals(testBean.getSpouse().getName())).as("Spouse copied").isTrue();
	}

	@Test
	public void testCopyPropertiesForImmutableBean() throws Exception {
		TestBean testBean = createTestBean("Bob", createTestBean("Mary"));

		ImmutableTestBean actual = underTest.transform(testBean, ImmutableTestBean.class);

		assertThat(actual.getName().equals(testBean.getName())).as("Name copied").isTrue();
		assertThat(actual.getAge() == testBean.getAge()).as("Age copied").isTrue();
		assertThat(actual.getTouchy().equals(testBean.getTouchy())).as("Touchy copied").isTrue();
		assertThat(actual.getSpouse().getName().equals(testBean.getSpouse().getName())).as("Spouse copied").isTrue();
	}

	@Test
	public void testCopyPropertiesUsingAPreConfiguredBeanTransformer() throws Exception {
		FieldTransformer<Integer, Integer> ageDoubler = new FieldTransformer<>("age", age -> age * 2);
		com.hotels.beans.transformer.BeanTransformer beanTransformer = BeanTransformer.getBeanTransformer()
				.withFieldTransformer(ageDoubler);
		TestBean testBean = createTestBean("Bob", createTestBean("Mary"));

		TestBean actual = underTest.transform(testBean, TestBean.class, beanTransformer);

		assertThat(actual.getName().equals(testBean.getName())).as("Name copied").isTrue();
		assertThat(actual.getAge() == (testBean.getAge() * 2)).as("Age copied and doubled").isTrue();
		assertThat(actual.getTouchy().equals(testBean.getTouchy())).as("Touchy copied").isTrue();
		assertThat(actual.getSpouse().getName().equals(testBean.getSpouse().getName())).as("Spouse copied").isTrue();
	}

	@Test
	public void testGetBeanTransformer() {
		assertThat(BeanTransformer.getBeanTransformer()).isNotNull();
	}

	private TestBean createTestBean(final String name) throws Exception {
		TestBean testBean = new TestBean();
		testBean.setName(name);
		testBean.setAge(32);
		testBean.setTouchy("touchy");
		return testBean;
	}

	private TestBean createTestBean(final String name, final TestBean spouse) throws Exception {
		TestBean testBean = createTestBean(name);
		testBean.setSpouse(spouse);
		return testBean;
	}
}
