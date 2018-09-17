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
package org.springframework.asm;

/**
 * Exception thrown when the constant pool of a class produced by a {@link ClassWriter} is too
 * large.
 *
 * @author Jason Zaugg
 */
@SuppressWarnings("serial")
public final class ClassTooLargeException extends IndexOutOfBoundsException {

  private final String className;
  private final int constantPoolCount;

  /**
   * Constructs a new {@link ClassTooLargeException}.
   *
   * @param className the internal name of the class.
   * @param constantPoolCount the number of constant pool items of the class.
   */
  public ClassTooLargeException(final String className, final int constantPoolCount) {
    super("Class too large: " + className);
    this.className = className;
    this.constantPoolCount = constantPoolCount;
  }

  /**
   * Returns the internal name of the class.
   *
   * @return the internal name of the class.
   */
  public String getClassName() {
    return className;
  }

  /**
   * Returns the number of constant pool items of the class.
   *
   * @return the number of constant pool items of the class.
   */
  public int getConstantPoolCount() {
    return constantPoolCount;
  }
}
