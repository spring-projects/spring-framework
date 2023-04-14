// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package org.springframework.asm.signature;

import org.springframework.asm.Opcodes;

/**
 * A SignatureVisitor that generates signature literals, as defined in the Java Virtual
 * Machine Specification (JVMS).
 *
 * @see <a href=
 * "https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9.1">JVMS
 * 4.7.9.1</a>
 * @author Thomas Hallgren
 * @author Eric Bruneton
 */
public class SignatureWriter extends SignatureVisitor {

	/** The builder used to construct the visited signature. */
	private final StringBuilder stringBuilder;

	/** Whether the visited signature contains formal type parameters. */
	private boolean hasFormals;

	/** Whether the visited signature contains method parameter types. */
	private boolean hasParameters;

	/**
	 * The stack used to keep track of class types that have arguments. Each element of
	 * this stack is a boolean encoded in one bit. The top of the stack is the least
	 * significant bit. The bottom of the stack is a sentinel element always equal to 1
	 * (used to detect when the stack is full). Pushing false = {@code <<= 1}, pushing
	 * true = {@code ( <<= 1) | 1}, popping = {@code >>>= 1}.
	 *
	 * <p>
	 * Class type arguments must be surrounded with '&lt;' and '&gt;' and, because
	 *
	 * <ol>
	 * <li>class types can be nested (because type arguments can themselves be class
	 * types),
	 * <li>SignatureWriter always returns 'this' in each visit* method (to avoid
	 * allocating new SignatureWriter instances),
	 * </ol>
	 *
	 * <p>
	 * we need a stack to properly balance these angle brackets. A new element is pushed
	 * on this stack for each new visited type, and popped when the visit of this type
	 * ends (either in visitEnd, or because visitInnerClassType is called).
	 */
	private int argumentStack = 1;

	/** Constructs a new {@link SignatureWriter}. */
	public SignatureWriter() {
		this(new StringBuilder());
	}

	private SignatureWriter(final StringBuilder stringBuilder) {
		super(/* latest api = */ Opcodes.ASM9);
		this.stringBuilder = stringBuilder;
	}

	// -----------------------------------------------------------------------------------------------
	// Implementation of the SignatureVisitor interface
	// -----------------------------------------------------------------------------------------------

	@Override
	public void visitFormalTypeParameter(final String name) {
		if (!hasFormals) {
			hasFormals = true;
			stringBuilder.append('<');
		}
		stringBuilder.append(name);
		stringBuilder.append(':');
	}

	@Override
	public SignatureVisitor visitClassBound() {
		return this;
	}

	@Override
	public SignatureVisitor visitInterfaceBound() {
		stringBuilder.append(':');
		return this;
	}

	@Override
	public SignatureVisitor visitSuperclass() {
		endFormals();
		return this;
	}

	@Override
	public SignatureVisitor visitInterface() {
		return this;
	}

	@Override
	public SignatureVisitor visitParameterType() {
		endFormals();
		if (!hasParameters) {
			hasParameters = true;
			stringBuilder.append('(');
		}
		return this;
	}

	@Override
	public SignatureVisitor visitReturnType() {
		endFormals();
		if (!hasParameters) {
			stringBuilder.append('(');
		}
		stringBuilder.append(')');
		return this;
	}

	@Override
	public SignatureVisitor visitExceptionType() {
		stringBuilder.append('^');
		return this;
	}

	@Override
	public void visitBaseType(final char descriptor) {
		stringBuilder.append(descriptor);
	}

	@Override
	public void visitTypeVariable(final String name) {
		stringBuilder.append('T');
		stringBuilder.append(name);
		stringBuilder.append(';');
	}

	@Override
	public SignatureVisitor visitArrayType() {
		stringBuilder.append('[');
		return this;
	}

	@Override
	public void visitClassType(final String name) {
		stringBuilder.append('L');
		stringBuilder.append(name);
		// Pushes 'false' on the stack, meaning that this type does not have type
		// arguments (as far as
		// we can tell at this point).
		argumentStack <<= 1;
	}

	@Override
	public void visitInnerClassType(final String name) {
		endArguments();
		stringBuilder.append('.');
		stringBuilder.append(name);
		// Pushes 'false' on the stack, meaning that this type does not have type
		// arguments (as far as
		// we can tell at this point).
		argumentStack <<= 1;
	}

	@Override
	public void visitTypeArgument() {
		// If the top of the stack is 'false', this means we are visiting the first type
		// argument of the
		// currently visited type. We therefore need to append a '<', and to replace the
		// top stack
		// element with 'true' (meaning that the current type does have type arguments).
		if ((argumentStack & 1) == 0) {
			argumentStack |= 1;
			stringBuilder.append('<');
		}
		stringBuilder.append('*');
	}

	@Override
	public SignatureVisitor visitTypeArgument(final char wildcard) {
		// If the top of the stack is 'false', this means we are visiting the first type
		// argument of the
		// currently visited type. We therefore need to append a '<', and to replace the
		// top stack
		// element with 'true' (meaning that the current type does have type arguments).
		if ((argumentStack & 1) == 0) {
			argumentStack |= 1;
			stringBuilder.append('<');
		}
		if (wildcard != '=') {
			stringBuilder.append(wildcard);
		}
		// If the stack is full, start a nested one by returning a new SignatureWriter.
		return (argumentStack & (1 << 31)) == 0 ? this : new SignatureWriter(stringBuilder);
	}

	@Override
	public void visitEnd() {
		endArguments();
		stringBuilder.append(';');
	}

	/**
	 * Returns the signature that was built by this signature writer.
	 * @return the signature that was built by this signature writer.
	 */
	@Override
	public String toString() {
		return stringBuilder.toString();
	}

	// -----------------------------------------------------------------------------------------------
	// Utility methods
	// -----------------------------------------------------------------------------------------------

	/** Ends the formal type parameters section of the signature. */
	private void endFormals() {
		if (hasFormals) {
			hasFormals = false;
			stringBuilder.append('>');
		}
	}

	/** Ends the type arguments of a class or inner class type. */
	private void endArguments() {
		// If the top of the stack is 'true', this means that some type arguments have
		// been visited for
		// the type whose visit is now ending. We therefore need to append a '>', and to
		// pop one element
		// from the stack.
		if ((argumentStack & 1) == 1) {
			stringBuilder.append('>');
		}
		argumentStack >>>= 1;
	}

}
