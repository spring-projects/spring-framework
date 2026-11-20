/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting.groovy;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.groovy.control.BytecodeProcessor;

/**
 * @author Juergen Hoeller
 */
public class MyBytecodeProcessor implements BytecodeProcessor {

	public final Set<String> processed = new HashSet<>();

	@Override
	public byte[] processBytecode(String name, byte[] original) {
		this.processed.add(name);
		return original;
	}

}
