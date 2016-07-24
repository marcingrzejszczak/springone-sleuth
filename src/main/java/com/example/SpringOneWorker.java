package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

/**
 * @author Marcin Grzejszczak
 */
@Component
public class SpringOneWorker {

	private final Tracer tracer;
	private final RestTemplate restTemplate;

	@Autowired
	public SpringOneWorker(Tracer tracer, RestTemplate restTemplate) {
		this.tracer = tracer;
		this.restTemplate = restTemplate;
	}

	@Async
	public void doSthVeryImportant() {
		Span span = this.tracer.createSpan("very_important_span_in_async");
		try {
			this.tracer.addTag("conference", "springone");
			Thread.sleep(2000);
			span.logEvent("learnt_spring_boot");
			Thread.sleep(2000);
			span.logEvent("learnt_spring_cloud");
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		finally {
			this.tracer.close(span);
		}
	}

	@HystrixCommand(groupKey = "groupKey", commandKey = "commandKey")
	public String callService1() {
		return restTemplate.getForObject("http://localhost:8081/start", String.class);
	}

	@HystrixCommand(groupKey = "groupKey", commandKey = "commandKey", fallbackMethod = "fallback")
	public String callUnknownService() {
		return restTemplate.getForObject("http://foobar/start", String.class);
	}

	public String fallback() {
		return "fallback";
	}
}
