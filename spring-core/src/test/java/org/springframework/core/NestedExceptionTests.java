/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.core;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import junit.framework.TestCase;
import junit.framework.Assert;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class NestedExceptionTests extends TestCase {

	@SuppressWarnings("serial")
	public void testNestedRuntimeExceptionWithNoRootCause() {
		String mesg = "mesg of mine";
		// Making a class abstract doesn't _really_ prevent instantiation :-)
		NestedRuntimeException nex = new NestedRuntimeException(mesg) {};
		assertNull(nex.getCause());
		assertEquals(nex.getMessage(), mesg);

		// Check printStackTrace
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(baos);
		nex.printStackTrace(pw);
		pw.flush();
		String stackTrace = new String(baos.toByteArray());
		assertFalse(stackTrace.indexOf(mesg) == -1);
	}

	@SuppressWarnings("serial")
	public void testNestedRuntimeExceptionWithRootCause() {
		String myMessage = "mesg for this exception";
		String rootCauseMesg = "this is the obscure message of the root cause";
		Exception rootCause = new Exception(rootCauseMesg);
		// Making a class abstract doesn't _really_ prevent instantiation :-)
		NestedRuntimeException nex = new NestedRuntimeException(myMessage, rootCause) {};
		Assert.assertEquals(nex.getCause(), rootCause);
		assertTrue(nex.getMessage().indexOf(myMessage) != -1);
		assertTrue(nex.getMessage().indexOf(rootCauseMesg) != -1);

		// check PrintStackTrace
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(baos);
		nex.printStackTrace(pw);
		pw.flush();
		String stackTrace = new String(baos.toByteArray());
		assertFalse(stackTrace.indexOf(rootCause.getClass().getName()) == -1);
		assertFalse(stackTrace.indexOf(rootCauseMesg) == -1);
	}

	@SuppressWarnings("serial")
	public void testNestedCheckedExceptionWithNoRootCause() {
		String mesg = "mesg of mine";
		// Making a class abstract doesn't _really_ prevent instantiation :-)
		NestedCheckedException nex = new NestedCheckedException(mesg) {};
		assertNull(nex.getCause());
		assertEquals(nex.getMessage(), mesg);

		// Check printStackTrace
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(baos);
		nex.printStackTrace(pw);
		pw.flush();
		String stackTrace = new String(baos.toByteArray());
		assertFalse(stackTrace.indexOf(mesg) == -1);
	}

	@SuppressWarnings("serial")
	public void testNestedCheckedExceptionWithRootCause() {
		String myMessage = "mesg for this exception";
		String rootCauseMesg = "this is the obscure message of the root cause";
		Exception rootCause = new Exception(rootCauseMesg);
		// Making a class abstract doesn't _really_ prevent instantiation :-)
		NestedCheckedException nex = new NestedCheckedException(myMessage, rootCause) {};
		Assert.assertEquals(nex.getCause(), rootCause);
		assertTrue(nex.getMessage().indexOf(myMessage) != -1);
		assertTrue(nex.getMessage().indexOf(rootCauseMesg) != -1);

		// check PrintStackTrace
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(baos);
		nex.printStackTrace(pw);
		pw.flush();
		String stackTrace = new String(baos.toByteArray());
		assertFalse(stackTrace.indexOf(rootCause.getClass().getName()) == -1);
		assertFalse(stackTrace.indexOf(rootCauseMesg) == -1);
	}

}
