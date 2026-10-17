package com.capricha.capricha_api.rest;

public class CheckInException extends RuntimeException {
    private final int status;
    private final String code;

    public CheckInException(int status, String code, String mensagem) {
        super(mensagem);
        this.status = status;
        this.code = code;
    }

    public int getStatus() { return status; }
    public String getCode() { return code; }
}
