package no.nav.peproxy;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.hotspot.DefaultExports;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        DefaultExports.initialize();
        SpringApplication.run(Application.class, args);
    }

    /**
     * Make sure spring uses the defaultRegistry
     */
    @Bean
    public CollectorRegistry collectorRegistry() {
        return CollectorRegistry.defaultRegistry;
    }
}