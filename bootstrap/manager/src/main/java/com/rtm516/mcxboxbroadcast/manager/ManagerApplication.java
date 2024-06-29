package com.rtm516.mcxboxbroadcast.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Arrays;
import java.util.List;


@SpringBootApplication
public class ManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ManagerApplication.class, args);
	}

	@Bean
	RouterFunction<ServerResponse> spaRouter() {
		ClassPathResource index = new ClassPathResource("static/index.html");
		List<String> extensions = Arrays.asList("js", "css", "ico", "png", "jpg", "gif");

		RequestPredicate spaPredicate = RequestPredicates.path("/api/**")
			.or(RequestPredicates.pathExtension(extensions::contains))
			.negate();

		return RouterFunctions.route()
			.resource(spaPredicate, index)
			.build();
	}
}
