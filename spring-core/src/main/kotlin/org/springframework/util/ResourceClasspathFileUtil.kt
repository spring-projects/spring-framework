package org.springframework.util

import java.io.File

/**
 * Class for reading files from classpath without defining classpath directory.
 * E.g. you can load json files from classpath easily with this class.
 * @param resourcePath path to resource in classpath.
 *
 * @author Alexander Ilinykh
 */
class ResourceClasspathFileUtil(resourcePath: String) :
	File(ResourceClasspathFileUtil::class.java.classLoader.getResource(resourcePath)!!.toURI())