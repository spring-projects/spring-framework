package org.springframework.beans.factory.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ByteArrayResource;

import test.beans.TestBean;

/**
 * Tests cornering SPR-7547.
 *
 * @author Chris Beams
 */
public class PropertyPlaceholderConfigurerTests {

	/**
	 * Prior to the fix for SPR-7547, the following would throw
	 * IllegalStateException because the PropertiesLoaderSupport base class
	 * assumed ByteArrayResource implements Resource.getFilename().  It does
	 * not, and AbstractResource.getFilename() is called instead, raising the
	 * exception.  The following now works, as getFilename() is called in a
	 * try/catch to check whether the resource is actually file-based or not.
	 *
	 * See SPR-7552, which suggests paths to address the root issue rather than
	 * just patching the problem.
	 */
	@Test
	public void repro() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				rootBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.name}").getBeanDefinition());
		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.setLocation(new ByteArrayResource("my.name=Inigo Montoya".getBytes()));
		ppc.postProcessBeanFactory(bf);

		TestBean testBean = bf.getBean(TestBean.class);
		assertThat(testBean.getName(), equalTo("Inigo Montoya"));
	}
}
