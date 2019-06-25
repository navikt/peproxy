package no.nav.peproxy.support.dto;

public class HttpResponse {

    private byte[] data;
    private String contentType;
    private int status;

    public HttpResponse(byte[] data, String contentType, int status) {
        this.data = data;
        this.contentType = contentType;
        this.status = status;
    }

    public byte[] getData() {
        return data;
    }

    public String getContentType() {
        return contentType;
    }

    public int getStatus() {
        return status;
    }
}
