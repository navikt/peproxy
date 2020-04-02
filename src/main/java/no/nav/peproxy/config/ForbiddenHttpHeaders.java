package no.nav.peproxy.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum ForbiddenHttpHeaders {
    Upgrade("Upgrade"),
    Via("Via"),
    Charset("Charset"),
    Referer("Referer"),
    TE("TE"),
    Trailer("Trailer"),
    Cookie("Cookie"),
    Cookie2("Cookie2"),
    Date("Date"),
    DNT("DNT"),
    Expect("Expect"),
    Accept_("Accept-"),
    Accept_Encoding("Accept-Encoding"),
    Access_Control_Request_Headers("Access-Control-Request-Headers"),
    Access_Control_Request_Method("Access-Control-Request-Method"),
    Connection("Connection"),
    Content_Length("Content-Length"),
    Feature_Policy("Feature-Policy"),
    Host("Host"),
    Keep_Alive("Keep-Alive"),
    Origin("Origin"),
    Proxy_("Proxy-"),
    Sec_("Sec-"),
    Transfer_Encoding("Transfer-Encoding");

    private String value;

    private ForbiddenHttpHeaders(String value) { this.value = value; }

    public static boolean checkIfHttpHeaderIsViable(String httpHeader) {

        List<String> values = Arrays.stream(ForbiddenHttpHeaders.values())
                .map(h -> h.value.toLowerCase())
                .collect(Collectors.toList());

        return !values.contains(httpHeader.toLowerCase());
    }
}


