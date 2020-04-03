package no.nav.peproxy.config;


import org.springframework.http.HttpHeaders;

public class Constants {
    public static final String DEFAULT_EXPIRE_SECONDS = "60";

    public static String EXTERNAL_HTTPHEADERS_AUTHORIZATION = "EXTERNAL_" + HttpHeaders.AUTHORIZATION;
    public static String HTTPHEADERS_TARGET = "target";
    public static String HTTPHEADERS_MAX_AGE = "max-age";
}

