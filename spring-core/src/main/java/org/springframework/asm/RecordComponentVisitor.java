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
 * @deprecated this API is experimental.
 */
@Deprecated
public abstract class RecordComponentVisitor {
  /**
   * The ASM API version implemented by this visitor. The value of this field must be {@link
   * Opcodes#ASM8_EXPERIMENTAL}.
   */
  protected final int api;

  /**
   * The record visitor to which this visitor must delegate method calls. May be {@literal null}.
   */
  /*package-private*/ RecordComponentVisitor delegate;

  /**
   * Constructs a new {@link RecordComponentVisitor}.
   *
   * @param api the ASM API version implemented by this visitor. Must be {@link
   *     Opcodes#ASM8_EXPERIMENTAL}.
   * @deprecated this API is experimental.
   */
  @Deprecated
  public RecordComponentVisitor(final int api) {
    this(api, null);
  }

  /**
   * Constructs a new {@link RecordComponentVisitor}.
   *
   * @param api the ASM API version implemented by this visitor. Must be {@link
   *     Opcodes#ASM8_EXPERIMENTAL}.
   * @param recordComponentVisitor the record component visitor to which this visitor must delegate
   *     method calls. May be null.
   * @deprecated this API is experimental.
   */
  @Deprecated
  public RecordComponentVisitor(
      final int api, final RecordComponentVisitor recordComponentVisitor) {
    if (api != Opcodes.ASM7
        && api != Opcodes.ASM6
        && api != Opcodes.ASM5
        && api != Opcodes.ASM4
        && api != Opcodes.ASM8_EXPERIMENTAL) {
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
   * @deprecated this API is experimental.
   */
  @Deprecated
  public RecordComponentVisitor getDelegateExperimental() {
    return delegate;
  }

  /**
   * Visits an annotation of the record component.
   *
   * @param descriptor the class descriptor of the annotation class.
   * @param visible {@literal true} if the annotation is visible at runtime.
   * @return a visitor to visit the annotation values, or {@literal null} if this visitor is not
   *     interested in visiting this annotation.
   * @deprecated this API is experimental.
   */
  @Deprecated
  public AnnotationVisitor visitAnnotationExperimental(
      final String descriptor, final boolean visible) {
    if (delegate != null) {
      return delegate.visitAnnotationExperimental(descriptor, visible);
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
   * @deprecated this API is experimental.
   */
  @Deprecated
  public AnnotationVisitor visitTypeAnnotationExperimental(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (delegate != null) {
      return delegate.visitTypeAnnotationExperimental(typeRef, typePath, descriptor, visible);
    }
    return null;
  }

  /**
   * Visits a non standard attribute of the record component.
   *
   * @param attribute an attribute.
   * @deprecated this API is experimental.
   */
  @Deprecated
  public void visitAttributeExperimental(final Attribute attribute) {
    if (delegate != null) {
      delegate.visitAttributeExperimental(attribute);
    }
  }

  /**
   * Visits the end of the record component. This method, which is the last one to be called, is
   * used to inform the visitor that everything have been visited.
   *
   * @deprecated this API is experimental.
   */
  @Deprecated
  public void visitEndExperimental() {
    if (delegate != null) {
      delegate.visitEndExperimental();
    }
  }
}
