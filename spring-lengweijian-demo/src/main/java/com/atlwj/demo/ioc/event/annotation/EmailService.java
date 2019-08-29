package com.atlwj.demo.ioc.event.annotation;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class EmailService implements ApplicationEventPublisherAware {

	private List<String> blackList = Arrays.asList("known.spammer@example.org","known.hacker@example.org","john.doe@example.org");
	private ApplicationEventPublisher publisher;
	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.publisher = applicationEventPublisher;
	}

	public void setBlackList(List<String> blackList) {
		this.blackList = blackList;
	}

	public void sendEmail(String address, String content) {
		if (blackList.contains(address)) {
			publisher.publishEvent(new BlackListEvent(this, address, content));
			return;
		}
		// send email...
		System.out.println("send email logic...");
	}
}
