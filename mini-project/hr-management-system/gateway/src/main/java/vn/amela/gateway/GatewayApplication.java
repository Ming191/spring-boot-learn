package vn.amela.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    /**
     * Starts the Spring Boot gateway application.
     *
     * @param args command-line arguments forwarded to the Spring Boot runtime
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
