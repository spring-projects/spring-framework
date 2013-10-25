/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.scripting.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import junit.framework.TestCase;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static org.mockito.BDDMockito.*;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class ResourceScriptSourceTests extends TestCase {

	public void testDoesNotPropagateFatalExceptionOnResourceThatCannotBeResolvedToAFile() throws Exception {
		Resource resource = mock(Resource.class);
		given(resource.lastModified()).willThrow(new IOException());

		ResourceScriptSource scriptSource = new ResourceScriptSource(resource);
		long lastModified = scriptSource.retrieveLastModifiedTime();
		assertEquals(0, lastModified);
	}

	public void testBeginsInModifiedState() throws Exception {
		Resource resource = mock(Resource.class);
		ResourceScriptSource scriptSource = new ResourceScriptSource(resource);
		assertTrue(scriptSource.isModified());
	}

	public void testLastModifiedWorksWithResourceThatDoesNotSupportFileBasedReading() throws Exception {
		Resource resource = mock(Resource.class);
		// underlying File is asked for so that the last modified time can be checked...
		// And then mock the file changing; i.e. the File says it has been modified
		given(resource.lastModified()).willReturn(100L, 100L, 200L);
		// does not support File-based reading; delegates to InputStream-style reading...
		//resource.getFile();
		//mock.setThrowable(new FileNotFoundException());
		given(resource.getInputStream()).willReturn(new ByteArrayInputStream(new byte[0]));

		ResourceScriptSource scriptSource = new ResourceScriptSource(resource);
		assertTrue("ResourceScriptSource must start off in the 'isModified' state (it obviously isn't).", scriptSource.isModified());
		scriptSource.getScriptAsString();
		assertFalse("ResourceScriptSource must not report back as being modified if the underlying File resource is not reporting a changed lastModified time.", scriptSource.isModified());
		// Must now report back as having been modified
		assertTrue("ResourceScriptSource must report back as being modified if the underlying File resource is reporting a changed lastModified time.", scriptSource.isModified());
	}

	public void testLastModifiedWorksWithResourceThatDoesNotSupportFileBasedAccessAtAll() throws Exception {
		Resource resource = new ByteArrayResource(new byte[0]);
		ResourceScriptSource scriptSource = new ResourceScriptSource(resource);
		assertTrue("ResourceScriptSource must start off in the 'isModified' state (it obviously isn't).", scriptSource.isModified());
		scriptSource.getScriptAsString();
		assertFalse("ResourceScriptSource must not report back as being modified if the underlying File resource is not reporting a changed lastModified time.", scriptSource.isModified());
		// Must now continue to report back as not having been modified 'cos the Resource does not support access as a File (and so the lastModified date cannot be determined).
		assertFalse("ResourceScriptSource must not report back as being modified if the underlying File resource is not reporting a changed lastModified time.", scriptSource.isModified());
	}

}
