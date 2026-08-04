package com.systemdesign.legacyinmemory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * Java port of {@code src/main.js} - boots the modular monolith and starts serving HTTP
 * (Spring's embedded Tomcat plays the role of {@code server.listen(PORT, ...)}), then prints
 * the same set of example {@code curl} commands the original prints once it's listening.
 *
 * <p>This banner only runs outside the {@code demo} profile (see
 * {@link com.systemdesign.legacyinmemory.demo.DemoRunner} for the {@code npm run demo}
 * equivalent) - {@code mvn spring-boot:run} with no profile is the direct equivalent of
 * {@code npm start} / {@code node src/main.js}.
 */
@SpringBootApplication
public class LegacyInMemoryDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegacyInMemoryDemoApplication.class, args);
    }

    @Bean
    @Profile("!demo")
    CommandLineRunner startupBanner(@Value("${server.port}") String port) {
        return args -> {
            System.out.println();
            System.out.println("Modular monolith demo listening on http://localhost:" + port);
            System.out.println("Try:");
            System.out.println("  curl http://localhost:" + port + "/products");
            System.out.println("  curl -X POST http://localhost:" + port
                    + "/auth/register -H \"Content-Type: application/json\" -d '{\"email\":\"a@b.com\",\"password\":\"secret\"}'");
            System.out.println("  curl -X POST http://localhost:" + port
                    + "/basket/1/items -H \"Content-Type: application/json\" -d '{\"productId\":1}'");
            System.out.println("  curl -X POST http://localhost:" + port + "/orders/1");
        };
    }
}
