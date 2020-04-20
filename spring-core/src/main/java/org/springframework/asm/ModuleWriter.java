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
 * A {@link ModuleVisitor} that generates the corresponding Module, ModulePackages and
 * ModuleMainClass attributes, as defined in the Java Virtual Machine Specification (JVMS).
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.25">JVMS
 *     4.7.25</a>
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.26">JVMS
 *     4.7.26</a>
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.27">JVMS
 *     4.7.27</a>
 * @author Remi Forax
 * @author Eric Bruneton
 */
final class ModuleWriter extends ModuleVisitor {

  /** Where the constants used in this AnnotationWriter must be stored. */
  private final SymbolTable symbolTable;

  /** The module_name_index field of the JVMS Module attribute. */
  private final int moduleNameIndex;

  /** The module_flags field of the JVMS Module attribute. */
  private final int moduleFlags;

  /** The module_version_index field of the JVMS Module attribute. */
  private final int moduleVersionIndex;

  /** The requires_count field of the JVMS Module attribute. */
  private int requiresCount;

  /** The binary content of the 'requires' array of the JVMS Module attribute. */
  private final ByteVector requires;

  /** The exports_count field of the JVMS Module attribute. */
  private int exportsCount;

  /** The binary content of the 'exports' array of the JVMS Module attribute. */
  private final ByteVector exports;

  /** The opens_count field of the JVMS Module attribute. */
  private int opensCount;

  /** The binary content of the 'opens' array of the JVMS Module attribute. */
  private final ByteVector opens;

  /** The uses_count field of the JVMS Module attribute. */
  private int usesCount;

  /** The binary content of the 'uses_index' array of the JVMS Module attribute. */
  private final ByteVector usesIndex;

  /** The provides_count field of the JVMS Module attribute. */
  private int providesCount;

  /** The binary content of the 'provides' array of the JVMS Module attribute. */
  private final ByteVector provides;

  /** The provides_count field of the JVMS ModulePackages attribute. */
  private int packageCount;

  /** The binary content of the 'package_index' array of the JVMS ModulePackages attribute. */
  private final ByteVector packageIndex;

  /** The main_class_index field of the JVMS ModuleMainClass attribute, or 0. */
  private int mainClassIndex;

  ModuleWriter(final SymbolTable symbolTable, final int name, final int access, final int version) {
    super(/* latest api = */ Opcodes.ASM7);
    this.symbolTable = symbolTable;
    this.moduleNameIndex = name;
    this.moduleFlags = access;
    this.moduleVersionIndex = version;
    this.requires = new ByteVector();
    this.exports = new ByteVector();
    this.opens = new ByteVector();
    this.usesIndex = new ByteVector();
    this.provides = new ByteVector();
    this.packageIndex = new ByteVector();
  }

  @Override
  public void visitMainClass(final String mainClass) {
    this.mainClassIndex = symbolTable.addConstantClass(mainClass).index;
  }

  @Override
  public void visitPackage(final String packaze) {
    packageIndex.putShort(symbolTable.addConstantPackage(packaze).index);
    packageCount++;
  }

  @Override
  public void visitRequire(final String module, final int access, final String version) {
    requires
        .putShort(symbolTable.addConstantModule(module).index)
        .putShort(access)
        .putShort(version == null ? 0 : symbolTable.addConstantUtf8(version));
    requiresCount++;
  }

  @Override
  public void visitExport(final String packaze, final int access, final String... modules) {
    exports.putShort(symbolTable.addConstantPackage(packaze).index).putShort(access);
    if (modules == null) {
      exports.putShort(0);
    } else {
      exports.putShort(modules.length);
      for (String module : modules) {
        exports.putShort(symbolTable.addConstantModule(module).index);
      }
    }
    exportsCount++;
  }

  @Override
  public void visitOpen(final String packaze, final int access, final String... modules) {
    opens.putShort(symbolTable.addConstantPackage(packaze).index).putShort(access);
    if (modules == null) {
      opens.putShort(0);
    } else {
      opens.putShort(modules.length);
      for (String module : modules) {
        opens.putShort(symbolTable.addConstantModule(module).index);
      }
    }
    opensCount++;
  }

  @Override
  public void visitUse(final String service) {
    usesIndex.putShort(symbolTable.addConstantClass(service).index);
    usesCount++;
  }

  @Override
  public void visitProvide(final String service, final String... providers) {
    provides.putShort(symbolTable.addConstantClass(service).index);
    provides.putShort(providers.length);
    for (String provider : providers) {
      provides.putShort(symbolTable.addConstantClass(provider).index);
    }
    providesCount++;
  }

  @Override
  public void visitEnd() {
    // Nothing to do.
  }

  /**
   * Returns the number of Module, ModulePackages and ModuleMainClass attributes generated by this
   * ModuleWriter.
   *
   * @return the number of Module, ModulePackages and ModuleMainClass attributes (between 1 and 3).
   */
  int getAttributeCount() {
    return 1 + (packageCount > 0 ? 1 : 0) + (mainClassIndex > 0 ? 1 : 0);
  }

  /**
   * Returns the size of the Module, ModulePackages and ModuleMainClass attributes generated by this
   * ModuleWriter. Also add the names of these attributes in the constant pool.
   *
   * @return the size in bytes of the Module, ModulePackages and ModuleMainClass attributes.
   */
  int computeAttributesSize() {
    symbolTable.addConstantUtf8(Constants.MODULE);
    // 6 attribute header bytes, 6 bytes for name, flags and version, and 5 * 2 bytes for counts.
    int size =
        22 + requires.length + exports.length + opens.length + usesIndex.length + provides.length;
    if (packageCount > 0) {
      symbolTable.addConstantUtf8(Constants.MODULE_PACKAGES);
      // 6 attribute header bytes, and 2 bytes for package_count.
      size += 8 + packageIndex.length;
    }
    if (mainClassIndex > 0) {
      symbolTable.addConstantUtf8(Constants.MODULE_MAIN_CLASS);
      // 6 attribute header bytes, and 2 bytes for main_class_index.
      size += 8;
    }
    return size;
  }

  /**
   * Puts the Module, ModulePackages and ModuleMainClass attributes generated by this ModuleWriter
   * in the given ByteVector.
   *
   * @param output where the attributes must be put.
   */
  void putAttributes(final ByteVector output) {
    // 6 bytes for name, flags and version, and 5 * 2 bytes for counts.
    int moduleAttributeLength =
        16 + requires.length + exports.length + opens.length + usesIndex.length + provides.length;
    output
        .putShort(symbolTable.addConstantUtf8(Constants.MODULE))
        .putInt(moduleAttributeLength)
        .putShort(moduleNameIndex)
        .putShort(moduleFlags)
        .putShort(moduleVersionIndex)
        .putShort(requiresCount)
        .putByteArray(requires.data, 0, requires.length)
        .putShort(exportsCount)
        .putByteArray(exports.data, 0, exports.length)
        .putShort(opensCount)
        .putByteArray(opens.data, 0, opens.length)
        .putShort(usesCount)
        .putByteArray(usesIndex.data, 0, usesIndex.length)
        .putShort(providesCount)
        .putByteArray(provides.data, 0, provides.length);
    if (packageCount > 0) {
      output
          .putShort(symbolTable.addConstantUtf8(Constants.MODULE_PACKAGES))
          .putInt(2 + packageIndex.length)
          .putShort(packageCount)
          .putByteArray(packageIndex.data, 0, packageIndex.length);
    }
    if (mainClassIndex > 0) {
      output
          .putShort(symbolTable.addConstantUtf8(Constants.MODULE_MAIN_CLASS))
          .putInt(2)
          .putShort(mainClassIndex);
    }
  }
}
