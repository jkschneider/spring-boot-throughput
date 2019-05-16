package com.example.demo;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;

public class DemoClient {
  public static void main(String[] args) {
    ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);

    PrometheusMeterRegistry meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    Counter counter = meterRegistry.counter("requests");

    scrapeEndpoint(meterRegistry);

    Hooks.onOperatorDebug();

    WebClient client = WebClient.builder()
      .baseUrl("http://localhost:8080")
      .build();

    Flux
      .generate(AtomicLong::new, (state, sink) -> {
        long i = state.getAndIncrement();
        sink.next(i);
        return state;
      })
      .limitRate(1)
      .flatMap(n -> client.get().uri("/persons").exchange())
      .doOnNext(resp -> {
        if (resp.statusCode().is2xxSuccessful())
          counter.increment();
      })
      .blockLast();

    System.out.println(counter.count());
  }

  private static void scrapeEndpoint(PrometheusMeterRegistry meterRegistry) {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
      server.createContext("/prometheus", httpExchange -> {
        String response = meterRegistry.scrape();
        httpExchange.sendResponseHeaders(200, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
      });

      new Thread(server::start).run();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
