package com.example;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanName;
import org.springframework.cloud.sleuth.SpanNamer;
import org.springframework.cloud.sleuth.TraceKeys;
import org.springframework.cloud.sleuth.TraceRunnable;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.hystrix.TraceCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;

/**
 * @author Marcin Grzejszczak
 */
@RestController
public class SpringOneController {

	private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

	private final RestTemplate restTemplate;
	private final Tracer tracer;
	private final SpanNamer spanNamer;
	private final SpringOneWorker springOneWorker;
	private final AsyncRestTemplate asyncRestTemplate;
	private final Service1Client service1Client;
	private final TraceKeys traceKeys;

	@Autowired
	public SpringOneController(RestTemplate restTemplate, Tracer tracer,
			SpanNamer spanNamer, SpringOneWorker springOneWorker,
			AsyncRestTemplate asyncRestTemplate, Service1Client service1Client,
			TraceKeys traceKeys) {
		this.restTemplate = restTemplate;
		this.tracer = tracer;
		this.spanNamer = spanNamer;
		this.springOneWorker = springOneWorker;
		this.asyncRestTemplate = asyncRestTemplate;
		this.service1Client = service1Client;
		this.traceKeys = traceKeys;
	}

	@RequestMapping("/springone")
	public String springOne() throws ExecutionException, InterruptedException {
		Future<?> future = EXECUTOR_SERVICE
				.submit(new TraceRunnable(this.tracer, this.spanNamer, new Runnable() {

					@Override public void run() {
						Span span = tracer.getCurrentSpan();
						try {
							tracer.addTag("conference", "springone");
							Thread.sleep(2000);
							span.logEvent("learnt_spring_boot");
							Thread.sleep(2000);
							span.logEvent("learnt_spring_cloud");
						}
						catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

					@Override public String toString() {
						return "poor_mans_runnable";
					}
				}));
		//future.get();
		String response = this.restTemplate.getForObject("http://localhost:8081/start", String.class);
		future.get();
		return "SPRINGONE [" + response + "]";
	}

	@RequestMapping("/springoneasync")
	public String springOneAsync() throws ExecutionException, InterruptedException {
		this.springOneWorker.doSthVeryImportant();
		String response = this.restTemplate.getForObject("http://localhost:8081/start", String.class);
		return "ASYNC SPRINGONE 1 [" + response + "]";
	}

	@RequestMapping("/springonecallable")
	public Callable<String> springOneCallable() throws ExecutionException, InterruptedException {
		return () -> {
			this.springOneWorker.doSthVeryImportant();
			String response = this.restTemplate.getForObject("http://localhost:8081/start", String.class);
			return "CALLABLE SPRINGONE 1 [" + response + "]";
		};
	}

	@RequestMapping("/springoneasynctemplate")
	public String springOneAsyncTemplate() throws ExecutionException, InterruptedException {
		ListenableFuture<ResponseEntity<String>> forEntity = this.asyncRestTemplate
				.getForEntity("http://localhost:8081/start", String.class);
		doSthVeryImportant();
		ResponseEntity<String> entity = forEntity.get();
		return "ASYNC SPRINGONE 2 [" + entity.getBody() + "]";
	}

	@RequestMapping("/springonefeign")
	public String springOneFeign() throws ExecutionException, InterruptedException {
		String response = service1Client.start();
		return "FEIGN SPRINGONE [" + response + "]";
	}

	@RequestMapping("/springonehystrix")
	public String springOneHystrix() throws Exception {
		return "HYSTRIX [" + new TraceCommand<String>(this.tracer, this.traceKeys,
				HystrixCommand.Setter.withGroupKey(
					HystrixCommandGroupKey.Factory.asKey("springone"))
					.andCommandKey(HystrixCommandKey.Factory.asKey("springonecommandkey"))) {
			@Override public String doRun() throws Exception {
				return restTemplate.getForObject("http://localhost:8081/start", String.class);
			}
		}.execute() + "]";
	}

	@RequestMapping("/springonejavanica")
	public String springOneJavanica() throws Exception {
		return "JAVANICA [" + this.springOneWorker.callService1() + "]";
	}

	@RequestMapping("/springonejavanicaunknown")
	public String springOneJavanicaUnknown() throws Exception {
		return "JAVANICA [" + this.springOneWorker.callUnknownService() + "]";
	}

	@SpanName("my_runnable_span")
	private static class MyRunnable implements Runnable {

		private final Tracer tracer;

		private MyRunnable(Tracer tracer) {
			this.tracer = tracer;
		}

		@Override public void run() {
			Span span = this.tracer.getCurrentSpan();
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

	public void doSthVeryImportant() {
		Span span = this.tracer.createSpan("very_important_span");
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
