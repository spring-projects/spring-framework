/*
 * Copyright 2002-2018 the original author or authors.
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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class NestedExceptionTests {

	@Test
	public void nestedRuntimeExceptionWithNoRootCause() {
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
		assertTrue(stackTrace.contains(mesg));
	}

	@Test
	public void nestedRuntimeExceptionWithRootCause() {
		String myMessage = "mesg for this exception";
		String rootCauseMsg = "this is the obscure message of the root cause";
		Exception rootCause = new Exception(rootCauseMsg);
		// Making a class abstract doesn't _really_ prevent instantiation :-)
		NestedRuntimeException nex = new NestedRuntimeException(myMessage, rootCause) {};
		assertEquals(nex.getCause(), rootCause);
		assertTrue(nex.getMessage().contains(myMessage));
		assertTrue(nex.getMessage().endsWith(rootCauseMsg));

		// check PrintStackTrace
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(baos);
		nex.printStackTrace(pw);
		pw.flush();
		String stackTrace = new String(baos.toByteArray());
		assertTrue(stackTrace.contains(rootCause.getClass().getName()));
		assertTrue(stackTrace.contains(rootCauseMsg));
	}

	@Test
	public void nestedCheckedExceptionWithNoRootCause() {
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
		assertTrue(stackTrace.contains(mesg));
	}

	@Test
	public void nestedCheckedExceptionWithRootCause() {
		String myMessage = "mesg for this exception";
		String rootCauseMsg = "this is the obscure message of the root cause";
		Exception rootCause = new Exception(rootCauseMsg);
		// Making a class abstract doesn't _really_ prevent instantiation :-)
		NestedCheckedException nex = new NestedCheckedException(myMessage, rootCause) {};
		assertEquals(nex.getCause(), rootCause);
		assertTrue(nex.getMessage().contains(myMessage));
		assertTrue(nex.getMessage().endsWith(rootCauseMsg));

		// check PrintStackTrace
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(baos);
		nex.printStackTrace(pw);
		pw.flush();
		String stackTrace = new String(baos.toByteArray());
		assertTrue(stackTrace.contains(rootCause.getClass().getName()));
		assertTrue(stackTrace.contains(rootCauseMsg));
	}

}
