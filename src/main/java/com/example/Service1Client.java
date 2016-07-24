package com.example;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Marcin Grzejszczak
 */
@FeignClient(name = "service1", url = "localhost:8081")
interface Service1Client {

	@RequestMapping("/start")
	String start();
}
