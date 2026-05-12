package vn.amela.authservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import vn.amela.authservice.security.JwtAuthenticationFilter;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false"
})
class AuthServiceApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
    }

    @Test
    void jwtAuthenticationFilterIsOnlyCreatedInsideSecurityChain() {
        assertThat(applicationContext.getBeansOfType(JwtAuthenticationFilter.class)).isEmpty();
    }
}
