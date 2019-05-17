package com.example.demo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.frigga.Names;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.prometheus.rsocket.PrometheusRSocketClient;
import io.prometheus.client.CollectorRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@SpringBootApplication
public class DemoApplication {
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	PrometheusMeterRegistry prometheusMeterRegistry(CollectorRegistry collectorRegistry) {
		PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, collectorRegistry, Clock.SYSTEM);

		PrometheusRSocketClient.connect(registry, "23.251.146.19", 7001)
			.retryBackoff(Long.MAX_VALUE, Duration.ofSeconds(10), Duration.ofMinutes(10))
			.subscribe();

		return registry;
	}

	@Bean
	MeterFilter commonTags(@Value("${cf.foundation:local}") String foundation,
												 @Value("${VCAP_APPLICATION:#{null}}") String app,
												 @Value("${CF_INSTANCE_INDEX:0}") String instanceIndex) {
		String serverGroup = Optional.ofNullable(app).map(app2 -> {
			try {
				Map<String, Object> vcapApplication = new ObjectMapper().readValue(app, new TypeReference<Map<String, Object>>() {
				});
				return (String) vcapApplication.getOrDefault("application_name", "unknown");
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}).orElse("unknown");

		Names names = Names.parseName(serverGroup);

		return MeterFilter.commonTags(Tags.of("foundation", foundation, "app", names.getApp(), "cluster", names.getCluster(),
			"cf.instance.number", instanceIndex));
	}
}

@RestController
class PersonController {
	@GetMapping("/persons")
	Flux<String> persons() {
		return Flux.just("timmers", "michael");
	}
}