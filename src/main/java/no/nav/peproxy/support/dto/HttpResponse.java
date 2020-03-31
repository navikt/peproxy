package no.nav.peproxy.support.dto;

import org.springframework.http.HttpStatus;

public class HttpResponse {

    private byte[] body;
    private String contentType;
    private int status;

    public HttpResponse(byte[] data, String contentType, int status) {
        this.body = data;
        this.contentType = contentType;
        this.status = status;
    }

    public byte[] getBody() {
        return body;
    }

    public String getContentType() {
        return contentType;
    }

    public int getStatus() {
        return status;
    }

    public boolean is2xxSuccessful() {
        return HttpStatus.valueOf(getStatus()).is2xxSuccessful();
    }
}
