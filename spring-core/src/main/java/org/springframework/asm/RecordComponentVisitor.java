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
 * A visitor to visit a record component. The methods of this class must be called in the following
 * order: ( {@code visitAnnotation} | {@code visitTypeAnnotation} | {@code visitAttribute} )* {@code
 * visitEnd}.
 *
 * @author Remi Forax
 * @author Eric Bruneton
 */
public abstract class RecordComponentVisitor {
  /**
   * The ASM API version implemented by this visitor. The value of this field must be one of {@link
   * Opcodes#ASM8} or {@link Opcodes#ASM9}.
   */
  protected final int api;

  /**
   * The record visitor to which this visitor must delegate method calls. May be {@literal null}.
   */
  /*package-private*/ RecordComponentVisitor delegate;

  /**
   * Constructs a new {@link RecordComponentVisitor}.
   *
   * @param api the ASM API version implemented by this visitor. Must be one of {@link Opcodes#ASM8}
   *     or {@link Opcodes#ASM9}.
   */
  public RecordComponentVisitor(final int api) {
    this(api, null);
  }

  /**
   * Constructs a new {@link RecordComponentVisitor}.
   *
   * @param api the ASM API version implemented by this visitor. Must be {@link Opcodes#ASM8}.
   * @param recordComponentVisitor the record component visitor to which this visitor must delegate
   *     method calls. May be null.
   */
  public RecordComponentVisitor(
      final int api, final RecordComponentVisitor recordComponentVisitor) {
    if (api != Opcodes.ASM9
        && api != Opcodes.ASM8
        && api != Opcodes.ASM7
        && api != Opcodes.ASM6
        && api != Opcodes.ASM5
        && api != Opcodes.ASM4
        && api != Opcodes.ASM10_EXPERIMENTAL) {
      throw new IllegalArgumentException("Unsupported api " + api);
    }
    // SPRING PATCH: no preview mode check for ASM experimental
    this.api = api;
    this.delegate = recordComponentVisitor;
  }

  /**
   * The record visitor to which this visitor must delegate method calls. May be {@literal null}.
   *
   * @return the record visitor to which this visitor must delegate method calls or {@literal null}.
   */
  public RecordComponentVisitor getDelegate() {
    return delegate;
  }

  /**
   * Visits an annotation of the record component.
   *
   * @param descriptor the class descriptor of the annotation class.
   * @param visible {@literal true} if the annotation is visible at runtime.
   * @return a visitor to visit the annotation values, or {@literal null} if this visitor is not
   *     interested in visiting this annotation.
   */
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    if (delegate != null) {
      return delegate.visitAnnotation(descriptor, visible);
    }
    return null;
  }

  /**
   * Visits an annotation on a type in the record component signature.
   *
   * @param typeRef a reference to the annotated type. The sort of this type reference must be
   *     {@link TypeReference#CLASS_TYPE_PARAMETER}, {@link
   *     TypeReference#CLASS_TYPE_PARAMETER_BOUND} or {@link TypeReference#CLASS_EXTENDS}. See
   *     {@link TypeReference}.
   * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
   *     static inner type within 'typeRef'. May be {@literal null} if the annotation targets
   *     'typeRef' as a whole.
   * @param descriptor the class descriptor of the annotation class.
   * @param visible {@literal true} if the annotation is visible at runtime.
   * @return a visitor to visit the annotation values, or {@literal null} if this visitor is not
   *     interested in visiting this annotation.
   */
  public AnnotationVisitor visitTypeAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (delegate != null) {
      return delegate.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }
    return null;
  }

  /**
   * Visits a non standard attribute of the record component.
   *
   * @param attribute an attribute.
   */
  public void visitAttribute(final Attribute attribute) {
    if (delegate != null) {
      delegate.visitAttribute(attribute);
    }
  }

  /**
   * Visits the end of the record component. This method, which is the last one to be called, is
   * used to inform the visitor that everything have been visited.
   */
  public void visitEnd() {
    if (delegate != null) {
      delegate.visitEnd();
    }
  }
}
