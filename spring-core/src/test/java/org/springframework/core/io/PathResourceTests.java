/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.io;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for the {@link PathResource} class.
 *
 * @author Philippe Marschall
 */
public class PathResourceTests {

	@Test
	public void testExampleXml() throws IOException {
		String path = "src/test/java/org/springframework/core/io/example.xml";
		WritableResource resource = new PathResource(path);
		
		assertTrue("resource exists", resource.exists());
		assertTrue("resource is readable", resource.isReadable());
		assertTrue("resource is readable", resource.isWritable());
		assertEquals("example.xml", resource.getFilename());
		
		long contentLength = resource.contentLength();
		assertThat("contentLength", contentLength, greaterThan(100L));
		assertThat("contentLength", contentLength, lessThan(1000L));
		
		assertEquals(new File(path), resource.getFile());
		
		assertEquals(resource, new PathResource(path));
	}
	
	@Test
	public void testParentFolder() throws IOException {
		String path = "src/test/java/org/springframework/core/io/";
		
		WritableResource resource = new PathResource(path);
		
		assertTrue("resource exists", resource.exists());
		assertFalse("resource is readable", resource.isReadable());
		assertFalse("resource is readable", resource.isWritable());
		assertEquals("io", resource.getFilename());
		
		assertEquals(new File(path), resource.getFile());
		
		assertEquals(resource, new PathResource(path));
	}
	
	@Test
	public void testCreateRelative() throws IOException {
		String path = "src/test/java/org/springframework/core/io";
		
		String relativePath = "../example.xml";
		Resource relativeResource = new PathResource(path).createRelative(relativePath);
		PathResource expected = new PathResource(
				"src/test/java/org/springframework/core/example.xml");
		assertEquals(expected, relativeResource);
		
		path = path + "/";
		
		relativeResource = new PathResource(path).createRelative(relativePath);
		assertEquals(expected, relativeResource);
		
	}
	

	@Test
	public void testNotExistingFile() throws IOException {
		String path = "src/test/java/DoesNotExist.xml";
		WritableResource resource = new PathResource(path);
		
		assertFalse("resource exists", resource.exists());
		assertFalse("resource is readable", resource.isReadable());
		assertFalse("resource is readable", resource.isWritable());
	}

}
