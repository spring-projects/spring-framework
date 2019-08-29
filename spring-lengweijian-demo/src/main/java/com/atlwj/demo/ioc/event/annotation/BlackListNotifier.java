package com.atlwj.demo.ioc.event.annotation;

import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author lengweijian
 */
@Component
public class BlackListNotifier{

	@EventListener
	public void processBlackListEvent(BlackListEvent event) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss SSS");
		System.out.println("processBlackListEvent....");
		// notify appropriate parties via notificationAddress..
		String format = simpleDateFormat.format(new Date(event.getTimestamp()));
		System.out.printf("当前系统时间： %s\n",format);
		if (event.getContent().startsWith("亲爱的")) {
			System.out.println("态度不错，这次就免了。。");
		}else {
			System.out.printf("当前邮件:%s  由于历史原因已经位列黑名单中，想永久解除黑名单请充值200¥",event.getAddress());
		}
	}
}
