package org.springframework.core.io.support;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.core.io.Resource;


public class ResourceArrayPropertyEditorTests {
	
	private ResourceArrayPropertyEditor editor = new ResourceArrayPropertyEditor();
	
	@Test
	public void testVanillaResource() throws Exception {
		editor.setAsText("classpath:org/springframework/core/io/support/ResourceArrayPropertyEditor.class");
		Resource[] resources = (Resource[]) editor.getValue();
		assertNotNull(resources);
		assertTrue(resources[0].exists());
	}

	@Test
	public void testPatternResource() throws Exception {
		// N.B. this will sometimes fail if you use classpath: instead of classpath*:.  
		// The result depends on the classpath - if test-classes are segregated from classes
		// and they come first on the classpath (like in Maven) then it breaks, if classes
		// comes first (like in Spring Build) then it is OK.
		editor.setAsText("classpath*:org/springframework/core/io/support/Resource*Editor.class");
		Resource[] resources = (Resource[]) editor.getValue();
		assertNotNull(resources);
		assertTrue(resources[0].exists());
	}

}
