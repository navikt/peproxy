package no.nav.peproxy.support.dto;

public class Error {

    private String errorType;
    private String errorMessage;

    public Error(String errorType, String errorMessage) {
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
