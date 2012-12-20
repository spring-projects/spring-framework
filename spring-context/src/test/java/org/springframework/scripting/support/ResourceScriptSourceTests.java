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

package org.springframework.scripting.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class ResourceScriptSourceTests extends TestCase {

	public void testCtorWithNullResource() throws Exception {
		try {
			new ResourceScriptSource(null);
			fail("Must have thrown exception by this point.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testDoesNotPropagateFatalExceptionOnResourceThatCannotBeResolvedToAFile() throws Exception {
		MockControl mock = MockControl.createControl(Resource.class);
		Resource resource = (Resource) mock.getMock();
		resource.lastModified();
		mock.setThrowable(new IOException());
		mock.replay();

		ResourceScriptSource scriptSource = new ResourceScriptSource(resource);
		long lastModified = scriptSource.retrieveLastModifiedTime();
		assertEquals(0, lastModified);
		mock.verify();
	}

	public void testBeginsInModifiedState() throws Exception {
		MockControl mock = MockControl.createControl(Resource.class);
		Resource resource = (Resource) mock.getMock();
		mock.replay();

		ResourceScriptSource scriptSource = new ResourceScriptSource(resource);
		assertTrue(scriptSource.isModified());
		mock.verify();
	}

	public void testLastModifiedWorksWithResourceThatDoesNotSupportFileBasedReading() throws Exception {
		MockControl mock = MockControl.createControl(Resource.class);
		Resource resource = (Resource) mock.getMock();
		// underlying File is asked for so that the last modified time can be checked...
		resource.lastModified();
		mock.setReturnValue(100, 2);
		// does not support File-based reading; delegates to InputStream-style reading...
		//resource.getFile();
		//mock.setThrowable(new FileNotFoundException());
		resource.getInputStream();
		mock.setReturnValue(new ByteArrayInputStream(new byte[0]));
		// And then mock the file changing; i.e. the File says it has been modified
		resource.lastModified();
		mock.setReturnValue(200);
		mock.replay();

		ResourceScriptSource scriptSource = new ResourceScriptSource(resource);
		assertTrue("ResourceScriptSource must start off in the 'isModified' state (it obviously isn't).", scriptSource.isModified());
		scriptSource.getScriptAsString();
		assertFalse("ResourceScriptSource must not report back as being modified if the underlying File resource is not reporting a changed lastModified time.", scriptSource.isModified());
		// Must now report back as having been modified
		assertTrue("ResourceScriptSource must report back as being modified if the underlying File resource is reporting a changed lastModified time.", scriptSource.isModified());
		mock.verify();
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
