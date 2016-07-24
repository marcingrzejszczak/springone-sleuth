package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author Marcin Grzejszczak
 */
@Component
class SpringOneWorker {

	private final Tracer tracer;

	@Autowired
	SpringOneWorker(Tracer tracer) {
		this.tracer = tracer;
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
}
