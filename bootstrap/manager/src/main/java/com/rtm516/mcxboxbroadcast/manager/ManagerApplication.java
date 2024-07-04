package com.rtm516.mcxboxbroadcast.manager;

import com.rtm516.mcxboxbroadcast.manager.config.MainConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Arrays;
import java.util.List;


@SpringBootApplication
@EnableConfigurationProperties(MainConfig.class)
public class ManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ManagerApplication.class, args);
	}

	@Bean
	RouterFunction<ServerResponse> spaRouter() {
		ClassPathResource index = new ClassPathResource("static/index.html");
		List<String> extensions = Arrays.asList("js", "css", "ico", "png", "jpg", "svg");

		RequestPredicate spaPredicate = RequestPredicates.path("/api/**")
			.or(RequestPredicates.pathExtension(extensions::contains))
			.negate();

		return RouterFunctions.route()
			.resource(spaPredicate, index)
			.build();
	}
}
