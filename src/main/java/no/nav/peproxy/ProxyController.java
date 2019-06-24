package no.nav.peproxy;

import io.micrometer.core.annotation.Timed;
import no.nav.security.oidc.api.Unprotected;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/proxy")
@Unprotected
public class ProxyController {

    @GetMapping
    @Timed(value = "proxy_timer", percentiles = {.5, .9, .99})
    public ResponseEntity isAlive() {
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
