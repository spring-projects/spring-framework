/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.mapping;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

/**
 * Thrown in a map operation fails.
 * @see Mapper#map(Object, Object)
 * @author Keith Donald
 */
public class MappingException extends RuntimeException {

	private List<MappingFailure> mappingFailures;

	public MappingException(List<MappingFailure> mappingFailures) {
		super((String) null);
		this.mappingFailures = mappingFailures;
	}

	public int getMappingFailureCount() {
		return this.mappingFailures.size();
	}

	public List<MappingFailure> getMappingFailures() {
		return this.mappingFailures;
	}

	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder(getMappingFailureCount() + " mapping failure(s) occurred:");
		int i = 1;
		for (Iterator<MappingFailure> it = this.mappingFailures.iterator(); it.hasNext(); i++) {
			MappingFailure failure = it.next();
			sb.append(" #").append(i + ") ").append(failure.getMessage());
			if (it.hasNext()) {
				sb.append(",");
			}
		}
		return sb.toString();
	}

	@Override
	public void printStackTrace(PrintStream ps) {
		super.printStackTrace(ps);
		synchronized (ps) {
			ps.println();
			ps.println("Mapping Failure Traces:");
			int i = 1;
			for (Iterator<MappingFailure> it = this.mappingFailures.iterator(); it.hasNext(); i++) {
				MappingFailure failure = it.next();
				ps.println("- MappingFailure #" + i + ":");
				Throwable t = failure.getCause();
				if (t != null) {
					t.printStackTrace(ps);
				} else {
					ps.println("null");
				}
			}
		}
	}

	@Override
	public void printStackTrace(PrintWriter pw) {
		super.printStackTrace(pw);
		synchronized (pw) {
			pw.println();
			pw.println("Mapping Failure Traces:");
			int i = 1;
			for (Iterator<MappingFailure> it = this.mappingFailures.iterator(); it.hasNext(); i++) {
				MappingFailure failure = it.next();
				pw.println("- MappingFailure #" + i + ":");
				Throwable t = failure.getCause();
				if (t != null) {
					t.printStackTrace(pw);
				} else {
					pw.println("null");
				}
			}
		}
	}

}
