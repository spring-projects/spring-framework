/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.springframework.asm;

/**
 * @author Remi Forax
 */
final class ModuleWriter extends ModuleVisitor {
    /**
     * The class writer to which this Module attribute must be added.
     */
    private final ClassWriter cw;
    
    /**
     * size in byte of the Module attribute.
     */
    int size;
    
    /**
     * Number of attributes associated with the current module
     * (Version, ConcealPackages, etc) 
     */
    int attributeCount;
    
    /**
     * Size in bytes of the attributes associated with the current module
     */
    int attributesSize;
    
    /**
     * module name index in the constant pool
     */
    private final int name;
    
    /**
     * module access flags
     */
    private final int access;
    
    /**
     * module version index in the constant pool or 0
     */
    private final int version;
    
    /**
     * module main class index in the constant pool or 0
     */
    private int mainClass;
    
    /**
     * number of packages
     */
    private int packageCount;
    
    /**
     * The packages in bytecode form. This byte vector only contains
     * the items themselves, the number of items is store in packageCount
     */
    private ByteVector packages;
    
    /**
     * number of requires items
     */
    private int requireCount;
    
    /**
     * The requires items in bytecode form. This byte vector only contains
     * the items themselves, the number of items is store in requireCount
     */
    private ByteVector requires;
    
    /**
     * number of exports items
     */
    private int exportCount;
    
    /**
     * The exports items in bytecode form. This byte vector only contains
     * the items themselves, the number of items is store in exportCount
     */
    private ByteVector exports;
    
    /**
     * number of opens items
     */
    private int openCount;
    
    /**
     * The opens items in bytecode form. This byte vector only contains
     * the items themselves, the number of items is store in openCount
     */
    private ByteVector opens;
    
    /**
     * number of uses items
     */
    private int useCount;
    
    /**
     * The uses items in bytecode form. This byte vector only contains
     * the items themselves, the number of items is store in useCount
     */
    private ByteVector uses;
    
    /**
     * number of provides items
     */
    private int provideCount;
    
    /**
     * The uses provides in bytecode form. This byte vector only contains
     * the items themselves, the number of items is store in provideCount
     */
    private ByteVector provides;
    
    ModuleWriter(final ClassWriter cw, final int name,
            final int access, final int version) {
        super(Opcodes.ASM6);
        this.cw = cw;
        this.size = 16;  // name + access + version + 5 counts
        this.name = name;
        this.access = access;
        this.version = version;
    }
    
    @Override
    public void visitMainClass(String mainClass) {
        if (this.mainClass == 0) { // protect against several calls to visitMainClass
            cw.newUTF8("ModuleMainClass");
            attributeCount++;
            attributesSize += 8;
        }
        this.mainClass = cw.newClass(mainClass);
    }
    
    @Override
    public void visitPackage(String packaze) {
        if (packages == null) { 
            // protect against several calls to visitPackage
            cw.newUTF8("ModulePackages");
            packages = new ByteVector();
            attributeCount++;
            attributesSize += 8;
        }
        packages.putShort(cw.newPackage(packaze));
        packageCount++;
        attributesSize += 2;
    }
    
    @Override
    public void visitRequire(String module, int access, String version) {
        if (requires == null) {
            requires = new ByteVector();
        }
        requires.putShort(cw.newModule(module))
                .putShort(access)
                .putShort(version == null? 0: cw.newUTF8(version));
        requireCount++;
        size += 6;
    }
    
    @Override
    public void visitExport(String packaze, int access, String... modules) {
        if (exports == null) {
            exports = new ByteVector();
        }
        exports.putShort(cw.newPackage(packaze)).putShort(access);
        if (modules == null) {
            exports.putShort(0);
            size += 6;
        } else {
            exports.putShort(modules.length);
            for(String module: modules) {
                exports.putShort(cw.newModule(module));
            }    
            size += 6 + 2 * modules.length; 
        }
        exportCount++;
    }
    
    @Override
    public void visitOpen(String packaze, int access, String... modules) {
        if (opens == null) {
            opens = new ByteVector();
        }
        opens.putShort(cw.newPackage(packaze)).putShort(access);
        if (modules == null) {
            opens.putShort(0);
            size += 6;
        } else {
            opens.putShort(modules.length);
            for(String module: modules) {
                opens.putShort(cw.newModule(module));
            }    
            size += 6 + 2 * modules.length; 
        }
        openCount++;
    }
    
    @Override
    public void visitUse(String service) {
        if (uses == null) {
            uses = new ByteVector();
        }
        uses.putShort(cw.newClass(service));
        useCount++;
        size += 2;
    }
    
    @Override
    public void visitProvide(String service, String... providers) {
        if (provides == null) {
            provides = new ByteVector();
        }
        provides.putShort(cw.newClass(service));
        provides.putShort(providers.length);
        for(String provider: providers) {
            provides.putShort(cw.newClass(provider));
        }
        provideCount++;
        size += 4 + 2 * providers.length; 
    }
    
    @Override
    public void visitEnd() {
        // empty
    }

    void putAttributes(ByteVector out) {
        if (mainClass != 0) {
            out.putShort(cw.newUTF8("ModuleMainClass")).putInt(2).putShort(mainClass);
        }
        if (packages != null) {
            out.putShort(cw.newUTF8("ModulePackages"))
               .putInt(2 + 2 * packageCount)
               .putShort(packageCount)
               .putByteArray(packages.data, 0, packages.length);
        }
    }

    void put(ByteVector out) {
        out.putInt(size);
        out.putShort(name).putShort(access).putShort(version);
        out.putShort(requireCount);
        if (requires != null) {
            out.putByteArray(requires.data, 0, requires.length);
        }
        out.putShort(exportCount);
        if (exports != null) {
            out.putByteArray(exports.data, 0, exports.length);
        }
        out.putShort(openCount);
        if (opens != null) {
            out.putByteArray(opens.data, 0, opens.length);
        }
        out.putShort(useCount);
        if (uses != null) {
            out.putByteArray(uses.data, 0, uses.length);
        }
        out.putShort(provideCount);
        if (provides != null) {
            out.putByteArray(provides.data, 0, provides.length);
        }
    }    
}
