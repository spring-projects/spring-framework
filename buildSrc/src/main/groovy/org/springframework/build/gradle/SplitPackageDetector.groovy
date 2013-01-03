/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.build.gradle

class SplitPackageDetector {

    private static final String HIDDEN_DIRECTORY_PREFIX = "."

    private static final String JAVA_FILE_SUFFIX = ".java"

    private static final String SRC_MAIN_JAVA = "src" + File.separator + "main" + File.separator + "java"

    private final Map<File, Set<String>> pkgMap = [:]

    private final logger

    SplitPackageDetector(baseDir, logger) {
        this.logger = logger
        dirList(baseDir).each { File dir ->
            def packages = getPackagesInDirectory(dir)
            if (!packages.isEmpty()) {
                pkgMap.put(dir, packages)
            }
        }
    }

    private File[] dirList(String dir) {
        dirList(new File(dir))
    }

    private File[] dirList(File dir) {
        dir.listFiles({ file -> file.isDirectory() && !file.getName().startsWith(HIDDEN_DIRECTORY_PREFIX) } as FileFilter)
    }

    private Set<String> getPackagesInDirectory(File dir) {
        def pkgs = new HashSet<String>()
        addPackagesInDirectory(pkgs, new File(dir, SRC_MAIN_JAVA), "")
        return pkgs;
    }

    boolean diagnoseSplitPackages() {
        def splitFound = false;
        def dirs = pkgMap.keySet().toArray()
        def numDirs = dirs.length
        for (int i = 0; i < numDirs - 1; i++) {
            for (int j = i + 1; j < numDirs - 1; j++) {
                def di = dirs[i]
                def pi = new HashSet(pkgMap.get(di))
                def dj = dirs[j]
                def pj = pkgMap.get(dj)
                pi.retainAll(pj)
                if (!pi.isEmpty()) {
                    logger.error("Packages $pi are split between directories '$di' and '$dj'")
                    splitFound = true
                }
            }
        }
        return splitFound
    }

    private void addPackagesInDirectory(HashSet<String> packages, File dir, String pkg) {
        def scanDir = new File(dir, pkg)
        def File[] javaFiles = scanDir.listFiles({ file -> !file.isDirectory() && file.getName().endsWith(JAVA_FILE_SUFFIX) } as FileFilter)
        if (javaFiles != null && javaFiles.length != 0) {
            packages.add(pkg)
        }
        dirList(scanDir).each { File subDir ->
            addPackagesInDirectory(packages, dir, pkg + File.separator + subDir.getName())
        }
    }
}