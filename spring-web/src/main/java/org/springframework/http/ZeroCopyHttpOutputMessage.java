/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http;

import java.io.File;
import java.nio.file.Path;

import reactor.core.publisher.Mono;

/**
 * Sub-interface of {@code ReactiveOutputMessage} that has support for "zero-copy"
 * file transfers.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 5.0
 * @see <a href="https://en.wikipedia.org/wiki/Zero-copy">Zero-copy</a>
 */
public interface ZeroCopyHttpOutputMessage extends ReactiveHttpOutputMessage {

	/**
	 * Use the given {@link File} to write the body of the message to the underlying
	 * HTTP layer.
	 * @param file the file to transfer
	 * @param position the position within the file from which the transfer is to begin
	 * @param count the number of bytes to be transferred
	 * @return a publisher that indicates completion or error.
	 */
	default Mono<Void> writeWith(File file, long position, long count) {
		return writeWith(file.toPath(), position, count);
	}

	/**
	 * Use the given {@link Path} to write the body of the message to the underlying
	 * HTTP layer.
	 * @param file the file to transfer
	 * @param position the position within the file from which the transfer is to begin
	 * @param count the number of bytes to be transferred
	 * @return a publisher that indicates completion or error.
	 * @since 5.1
	 */
	Mono<Void> writeWith(Path file, long position, long count);

}
