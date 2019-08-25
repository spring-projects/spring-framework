/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.scripting.support;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class ResourceScriptSourceTests {

	@Test
	public void doesNotPropagateFatalExceptionOnResourceThatCannotBeResolvedToAFile() throws Exception {
		Resource resource = mock(Resource.class);
		given(resource.lastModified()).willThrow(new IOException());

		ResourceScriptSource scriptSource = new ResourceScriptSource(resource);
		long lastModified = scriptSource.retrieveLastModifiedTime();
		assertThat(lastModified).isEqualTo(0);
	}

	@Test
	public void beginsInModifiedState() throws Exception {
		Resource resource = mock(Resource.class);
		ResourceScriptSource scriptSource = new ResourceScriptSource(resource);
		assertThat(scriptSource.isModified()).isTrue();
	}

	@Test
	public void lastModifiedWorksWithResourceThatDoesNotSupportFileBasedReading() throws Exception {
		Resource resource = mock(Resource.class);
		// underlying File is asked for so that the last modified time can be checked...
		// And then mock the file changing; i.e. the File says it has been modified
		given(resource.lastModified()).willReturn(100L, 100L, 200L);
		// does not support File-based reading; delegates to InputStream-style reading...
		//resource.getFile();
		//mock.setThrowable(new FileNotFoundException());
		given(resource.getInputStream()).willReturn(StreamUtils.emptyInput());

		ResourceScriptSource scriptSource = new ResourceScriptSource(resource);
		assertThat(scriptSource.isModified()).as("ResourceScriptSource must start off in the 'isModified' state (it obviously isn't).").isTrue();
		scriptSource.getScriptAsString();
		assertThat(scriptSource.isModified()).as("ResourceScriptSource must not report back as being modified if the underlying File resource is not reporting a changed lastModified time.").isFalse();
		// Must now report back as having been modified
		assertThat(scriptSource.isModified()).as("ResourceScriptSource must report back as being modified if the underlying File resource is reporting a changed lastModified time.").isTrue();
	}

	@Test
	public void lastModifiedWorksWithResourceThatDoesNotSupportFileBasedAccessAtAll() throws Exception {
		Resource resource = new ByteArrayResource(new byte[0]);
		ResourceScriptSource scriptSource = new ResourceScriptSource(resource);
		assertThat(scriptSource.isModified()).as("ResourceScriptSource must start off in the 'isModified' state (it obviously isn't).").isTrue();
		scriptSource.getScriptAsString();
		assertThat(scriptSource.isModified()).as("ResourceScriptSource must not report back as being modified if the underlying File resource is not reporting a changed lastModified time.").isFalse();
		// Must now continue to report back as not having been modified 'cos the Resource does not support access as a File (and so the lastModified date cannot be determined).
		assertThat(scriptSource.isModified()).as("ResourceScriptSource must not report back as being modified if the underlying File resource is not reporting a changed lastModified time.").isFalse();
	}

}
