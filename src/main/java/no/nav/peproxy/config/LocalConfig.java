package no.nav.peproxy.config;

import no.nav.security.oidc.test.support.spring.TokenGeneratorConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Profile("local")
@Configuration
@Import(TokenGeneratorConfiguration.class)
public class LocalConfig {

}
