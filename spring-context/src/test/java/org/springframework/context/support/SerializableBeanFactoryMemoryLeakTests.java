package org.springframework.context.support;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Unit tests cornering SPR-7502.
 *
 * @author Chris Beams
 */
public class SerializableBeanFactoryMemoryLeakTests {

	/**
	 * Defensively zero-out static factory count - other tests
	 * may have misbehaved before us.
	 */
	@BeforeClass
	@AfterClass
	public static void zeroOutFactoryCount() throws Exception {
		getSerializableFactoryMap().clear();
	}

	@Test
	public void genericContext() throws Exception {
		assertFactoryCountThroughoutLifecycle(new GenericApplicationContext());
	}

	@Test
	public void abstractRefreshableContext() throws Exception {
		assertFactoryCountThroughoutLifecycle(new ClassPathXmlApplicationContext());
	}

	@Test
	public void genericContextWithMisconfiguredBean() throws Exception {
		GenericApplicationContext ctx = new GenericApplicationContext();
		registerMisconfiguredBeanDefinition(ctx);
		assertFactoryCountThroughoutLifecycle(ctx);
	}

	@Test
	public void abstractRefreshableContextWithMisconfiguredBean() throws Exception {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext() {
			@Override
			protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
				super.customizeBeanFactory(beanFactory);
				registerMisconfiguredBeanDefinition(beanFactory);
			}
		};
		assertFactoryCountThroughoutLifecycle(ctx);
	}

	private void assertFactoryCountThroughoutLifecycle(ConfigurableApplicationContext ctx) throws Exception {
		assertThat(serializableFactoryCount(), equalTo(0));
		try {
			ctx.refresh();
			assertThat(serializableFactoryCount(), equalTo(1));
			ctx.close();
		} catch (BeanCreationException ex) {
			// ignore - this is expected on refresh() for failure case tests
		} finally {
			assertThat(serializableFactoryCount(), equalTo(0));
		}
	}

	private void registerMisconfiguredBeanDefinition(BeanDefinitionRegistry registry) {
		registry.registerBeanDefinition("misconfigured",
			rootBeanDefinition(Object.class).addPropertyValue("nonexistent", "bogus")
				.getBeanDefinition());
	}

	private int serializableFactoryCount() throws Exception {
		Map<?, ?> map = getSerializableFactoryMap();
		return map.size();
	}

	private static Map<?, ?> getSerializableFactoryMap() throws Exception {
		Field field = DefaultListableBeanFactory.class.getDeclaredField("serializableFactories");
		field.setAccessible(true);
		return (Map<?, ?>) field.get(DefaultListableBeanFactory.class);
	}

}
