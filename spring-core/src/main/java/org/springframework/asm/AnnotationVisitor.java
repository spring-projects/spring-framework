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
 * A visitor to visit a Java annotation. The methods of this class must be called in the following
 * order: ( {@code visit} | {@code visitEnum} | {@code visitAnnotation} | {@code visitArray} )*
 * {@code visitEnd}.
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
public abstract class AnnotationVisitor {

  /**
   * The ASM API version implemented by this visitor. The value of this field must be one of the
   * {@code ASM}<i>x</i> values in {@link Opcodes}.
   */
  protected final int api;

  /**
   * The annotation visitor to which this visitor must delegate method calls. May be {@literal
   * null}.
   */
  protected AnnotationVisitor av;

  /**
   * Constructs a new {@link AnnotationVisitor}.
   *
   * @param api the ASM API version implemented by this visitor. Must be one of the {@code
   *     ASM}<i>x</i> values in {@link Opcodes}.
   */
  protected AnnotationVisitor(final int api) {
    this(api, null);
  }

  /**
   * Constructs a new {@link AnnotationVisitor}.
   *
   * @param api the ASM API version implemented by this visitor. Must be one of the {@code
   *     ASM}<i>x</i> values in {@link Opcodes}.
   * @param annotationVisitor the annotation visitor to which this visitor must delegate method
   *     calls. May be {@literal null}.
   */
  protected AnnotationVisitor(final int api, final AnnotationVisitor annotationVisitor) {
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
    this.av = annotationVisitor;
  }

  /**
   * Visits a primitive value of the annotation.
   *
   * @param name the value name.
   * @param value the actual value, whose type must be {@link Byte}, {@link Boolean}, {@link
   *     Character}, {@link Short}, {@link Integer} , {@link Long}, {@link Float}, {@link Double},
   *     {@link String} or {@link Type} of {@link Type#OBJECT} or {@link Type#ARRAY} sort. This
   *     value can also be an array of byte, boolean, short, char, int, long, float or double values
   *     (this is equivalent to using {@link #visitArray} and visiting each array element in turn,
   *     but is more convenient).
   */
  public void visit(final String name, final Object value) {
    if (av != null) {
      av.visit(name, value);
    }
  }

  /**
   * Visits an enumeration value of the annotation.
   *
   * @param name the value name.
   * @param descriptor the class descriptor of the enumeration class.
   * @param value the actual enumeration value.
   */
  public void visitEnum(final String name, final String descriptor, final String value) {
    if (av != null) {
      av.visitEnum(name, descriptor, value);
    }
  }

  /**
   * Visits a nested annotation value of the annotation.
   *
   * @param name the value name.
   * @param descriptor the class descriptor of the nested annotation class.
   * @return a visitor to visit the actual nested annotation value, or {@literal null} if this
   *     visitor is not interested in visiting this nested annotation. <i>The nested annotation
   *     value must be fully visited before calling other methods on this annotation visitor</i>.
   */
  public AnnotationVisitor visitAnnotation(final String name, final String descriptor) {
    if (av != null) {
      return av.visitAnnotation(name, descriptor);
    }
    return null;
  }

  /**
   * Visits an array value of the annotation. Note that arrays of primitive values (such as byte,
   * boolean, short, char, int, long, float or double) can be passed as value to {@link #visit
   * visit}. This is what {@link ClassReader} does for non empty arrays of primitive values.
   *
   * @param name the value name.
   * @return a visitor to visit the actual array value elements, or {@literal null} if this visitor
   *     is not interested in visiting these values. The 'name' parameters passed to the methods of
   *     this visitor are ignored. <i>All the array values must be visited before calling other
   *     methods on this annotation visitor</i>.
   */
  public AnnotationVisitor visitArray(final String name) {
    if (av != null) {
      return av.visitArray(name);
    }
    return null;
  }

  /** Visits the end of the annotation. */
  public void visitEnd() {
    if (av != null) {
      av.visitEnd();
    }
  }
}
