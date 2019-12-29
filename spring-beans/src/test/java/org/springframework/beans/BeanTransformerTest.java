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
		TestBean tb = new TestBean();
		tb.setName("rod");
		tb.setAge(32);
		tb.setTouchy("touchy");
		TestBean tb2 = underTest.transform(tb, TestBean.class);
		assertThat(tb2.getName().equals(tb.getName())).as("Name copied").isTrue();
		assertThat(tb2.getAge() == tb.getAge()).as("Age copied").isTrue();
		assertThat(tb2.getTouchy().equals(tb.getTouchy())).as("Touchy copied").isTrue();
	}

	@Test
	public void testCopyPropertiesForImmutableBean() throws Exception {
		TestBean tb = new TestBean();
		tb.setName("rod");
		tb.setAge(32);
		tb.setTouchy("touchy");
		TestBean spouse = new TestBean();
		spouse.setName("mary");
		tb.setSpouse(spouse);
		ImmutableTestBean itb = underTest.transform(tb, ImmutableTestBean.class);
		assertThat(itb.getName().equals(tb.getName())).as("Name copied").isTrue();
		assertThat(itb.getAge() == tb.getAge()).as("Age copied").isTrue();
		assertThat(itb.getTouchy().equals(tb.getTouchy())).as("Touchy copied").isTrue();
		assertThat(itb.getSpouse().getName().equals(tb.getSpouse().getName())).as("Spouse copied").isTrue();
	}

	@Test
	public void testCopyPropertiesUsingAPreConfiguredBeanTransformer() throws Exception {
		FieldTransformer<Integer, Integer> ageDoubler = new FieldTransformer<>("age", age -> age * 2);
		com.hotels.beans.transformer.BeanTransformer beanTransformer = BeanTransformer.getBeanTransformer()
				.withFieldTransformer(ageDoubler);
		TestBean tb = new TestBean();
		tb.setName("rod");
		tb.setAge(32);
		tb.setTouchy("touchy");
		TestBean tb2 = underTest.transform(tb, TestBean.class, beanTransformer);
		assertThat(tb2.getName().equals(tb.getName())).as("Name copied").isTrue();
		assertThat(tb2.getAge() == (tb.getAge() * 2)).as("Age copied and doubled").isTrue();
		assertThat(tb2.getTouchy().equals(tb.getTouchy())).as("Touchy copied").isTrue();
	}

	@Test
	public void testGetBeanTransformer() {
		assertThat(BeanTransformer.getBeanTransformer()).isNotNull();
	}
}
