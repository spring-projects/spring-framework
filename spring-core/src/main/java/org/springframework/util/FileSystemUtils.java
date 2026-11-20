/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import org.springframework.lang.Contract;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

/**
 * Utility methods for working with the file system.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.5.3
 * @see java.io.File
 * @see java.nio.file.Path
 * @see java.nio.file.Files
 */
public abstract class FileSystemUtils {

	/**
	 * Delete the supplied {@link File} - for directories,
	 * recursively delete any nested directories or files as well.
	 * <p>Note: Like {@link File#delete()}, this method does not throw any
	 * exception but rather silently returns {@code false} in case of I/O
	 * errors. Consider using {@link #deleteRecursively(Path)} for NIO-style
	 * handling of I/O errors, clearly differentiating between non-existence
	 * and failure to delete an existing file.
	 * @param root the root {@code File} to delete
	 * @return {@code true} if the {@code File} was successfully deleted,
	 * otherwise {@code false}
	 */
	@Contract("null -> false")
	public static boolean deleteRecursively(@Nullable File root) {
		if (root == null) {
			return false;
		}

		try {
			return deleteRecursively(root.toPath());
		}
		catch (IOException ex) {
			return false;
		}
	}

	/**
	 * Delete the supplied {@link Path} &mdash; for directories,
	 * recursively delete any nested directories or files as well.
	 * @param root the root {@code Path} to delete
	 * @return {@code true} if the {@code Path} existed and was deleted,
	 * or {@code false} if it did not exist
	 * @throws IOException in the case of I/O errors
	 * @since 5.0
	 */
	@Contract("null -> false")
	public static boolean deleteRecursively(@Nullable Path root) throws IOException {
		if (root == null) {
			return false;
		}
		if (!Files.exists(root)) {
			return false;
		}

		Files.walkFileTree(root, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException ex) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
		return true;
	}

	/**
	 * Recursively copy the contents of the {@code src} file/directory
	 * to the {@code dest} file/directory.
	 * @param src the source directory
	 * @param dest the destination directory
	 * @throws IOException in the case of I/O errors
	 */
	public static void copyRecursively(File src, File dest) throws IOException {
		Assert.notNull(src, "Source File must not be null");
		Assert.notNull(dest, "Destination File must not be null");
		copyRecursively(src.toPath(), dest.toPath());
	}

	/**
	 * Recursively copy the contents of the {@code src} file/directory
	 * to the {@code dest} file/directory, including symbolic links.
	 * @param src the source directory
	 * @param dest the destination directory
	 * @throws IOException in the case of I/O errors
	 * @since 5.0
	 */
	public static void copyRecursively(Path src, Path dest) throws IOException {
		Assert.notNull(src, "Source Path must not be null");
		Assert.notNull(dest, "Destination Path must not be null");
		BasicFileAttributes srcAttr = Files.readAttributes(src, BasicFileAttributes.class);

		if (srcAttr.isDirectory()) {
			if (src.getClass() == dest.getClass()) {  // dest.resolve(Path) only works for same Path type
				Files.walkFileTree(src, EnumSet.of(FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attr) throws IOException {
						Files.createDirectories(dest.resolve(src.relativize(dir)));
						return FileVisitResult.CONTINUE;
					}
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
						Files.copy(file, dest.resolve(src.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
						return FileVisitResult.CONTINUE;
					}
				});
			}
			else {  // use dest.resolve(String) for different Path types
				Files.walkFileTree(src, EnumSet.of(FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attr) throws IOException {
						Files.createDirectories(dest.resolve(src.relativize(dir).toString()));
						return FileVisitResult.CONTINUE;
					}
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
						Files.copy(file, dest.resolve(src.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING);
						return FileVisitResult.CONTINUE;
					}
				});
			}
		}
		else if (srcAttr.isRegularFile()) {
			Files.copy(src, dest);
		}
		else {
			throw new IllegalArgumentException("Source File must denote a directory or file");
		}
	}

}
