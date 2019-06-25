package no.nav.peproxy.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.peproxy.support.dto.Error;

public final class JsonUtils {

    private JsonUtils() {
    }

    private static ObjectMapper om = new ObjectMapper();

    public static String toJson(Exception e) {
        try {
            return om.writeValueAsString(new Error(e.getClass().getSimpleName(), e.getMessage()));
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("serialization error", e);
        }
    }
}
