/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.sockjs.transport.session;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.sockjs.frame.JacksonJsonSockJsMessageCodec;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;

/**
 * @author Rossen Stoyanchev
 */
public class StubSockJsServiceConfig implements SockJsServiceConfig {

	private int streamBytesLimit = 128 * 1024;

	private long heartbeatTime = 25 * 1000;

	private TaskScheduler taskScheduler = new ThreadPoolTaskScheduler();

	private SockJsMessageCodec messageCodec = new JacksonJsonSockJsMessageCodec();

	private int httpMessageCacheSize = 100;


	@Override
	public int getStreamBytesLimit() {
		return this.streamBytesLimit;
	}

	public void setStreamBytesLimit(int streamBytesLimit) {
		this.streamBytesLimit = streamBytesLimit;
	}

	@Override
	public long getHeartbeatTime() {
		return this.heartbeatTime;
	}

	public void setHeartbeatTime(long heartbeatTime) {
		this.heartbeatTime = heartbeatTime;
	}

	@Override
	public TaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}

	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	@Override
	public SockJsMessageCodec getMessageCodec() {
		return this.messageCodec;
	}

	public void setMessageCodec(SockJsMessageCodec messageCodec) {
		this.messageCodec = messageCodec;
	}

	@Override
	public int getHttpMessageCacheSize() {
		return this.httpMessageCacheSize;
	}

	public void setHttpMessageCacheSize(int httpMessageCacheSize) {
		this.httpMessageCacheSize = httpMessageCacheSize;
	}

}
