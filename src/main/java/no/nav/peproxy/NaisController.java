package no.nav.peproxy;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/")
public class NaisController {

    @GetMapping(path = "isAlive")
    public ResponseEntity isAlive() {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(path = "isReady")
    public ResponseEntity isReady() {
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
