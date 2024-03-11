package org.springframework.beans.factory.support;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Stream;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.springframework.lang.Nullable;

class CglibSubclassingInstantiationStrategyTests {

	private final CglibSubclassingInstantiationStrategy strategy = new CglibSubclassingInstantiationStrategy();

	@Nullable
	public static Object valueToReturnFromReplacer;

	@Test
	void methodOverride() {
		StaticListableBeanFactory beanFactory = new StaticListableBeanFactory(Map.of(
				"myBean", new MyBean(),
				"replacer", new MyReplacer()
		));

		RootBeanDefinition bd = new RootBeanDefinition(MyBean.class);
		MethodOverrides methodOverrides = new MethodOverrides();
		Stream.of("getBoolean", "getShort", "getInt", "getLong", "getFloat", "getDouble", "getByte")
				.forEach(methodToOverride -> addOverride(methodOverrides, methodToOverride));
		bd.setMethodOverrides(methodOverrides);

		MyBean bean = (MyBean) strategy.instantiate(bd, "myBean", beanFactory);

		valueToReturnFromReplacer = null;
		assertCorrectExceptionThrownBy(bean::getBoolean);
		valueToReturnFromReplacer = true;
		assertThat(bean.getBoolean()).isTrue();

		valueToReturnFromReplacer = null;
		assertCorrectExceptionThrownBy(bean::getShort);
		valueToReturnFromReplacer = 123;
		assertThat(bean.getShort()).isEqualTo((short) 123);

		valueToReturnFromReplacer = null;
		assertCorrectExceptionThrownBy(bean::getInt);
		valueToReturnFromReplacer = 123;
		assertThat(bean.getInt()).isEqualTo(123);

		valueToReturnFromReplacer = null;
		assertCorrectExceptionThrownBy(bean::getLong);
		valueToReturnFromReplacer = 123;
		assertThat(bean.getLong()).isEqualTo(123L);

		valueToReturnFromReplacer = null;
		assertCorrectExceptionThrownBy(bean::getFloat);
		valueToReturnFromReplacer = 123;
		assertThat(bean.getFloat()).isEqualTo(123f);

		valueToReturnFromReplacer = null;
		assertCorrectExceptionThrownBy(bean::getDouble);
		valueToReturnFromReplacer = 123;
		assertThat(bean.getDouble()).isEqualTo(123d);

		valueToReturnFromReplacer = null;
		assertCorrectExceptionThrownBy(bean::getByte);
		valueToReturnFromReplacer = 123;
		assertThat(bean.getByte()).isEqualTo((byte) 123);
	}

	private void assertCorrectExceptionThrownBy(ThrowableAssert.ThrowingCallable runnable) {
		assertThatThrownBy(runnable)
				.isInstanceOf(NullPointerException.class)
				.hasMessageMatching("Null return value from replacer does not match primitive return type for: "
				                    + "\\w+ org\\.springframework\\.beans\\.factory\\.support\\.CglibSubclassingInstantiationStrategyTests\\$MyBean\\.\\w+\\(\\)");
	}

	private void addOverride(MethodOverrides methodOverrides, String methodToOverride) {
		methodOverrides.addOverride(new ReplaceOverride(methodToOverride, "replacer"));
	}

	static class MyBean {
		boolean getBoolean() {
			return true;
		}

		short getShort() {
			return 123;
		}

		int getInt() {
			return 123;
		}

		long getLong() {
			return 123;
		}

		float getFloat() {
			return 123;
		}

		double getDouble() {
			return 123;
		}

		byte getByte() {
			return 123;
		}
	}

	static class MyReplacer implements MethodReplacer {

		@Override
		public Object reimplement(Object obj, Method method, Object[] args) {
			return CglibSubclassingInstantiationStrategyTests.valueToReturnFromReplacer;
		}
	}
}
