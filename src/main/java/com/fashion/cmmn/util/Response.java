package com.fashion.cmmn.util;

// Ajax 공통 응답 포맷. code: Constants.SUCCESS/FAIL 등, message: 사용자에게 보여줄 문구, data: 결과
public class Response {

    private final String code;
    private final String message;
    private final Object data;

    private Response(String code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static Response of(String code) {
        return new Response(code, null, null);
    }

    public static Response of(String code, Object data) {
        return new Response(code, null, data);
    }

    public static Response of(String code, String message, Object data) {
        return new Response(code, message, data);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }
}
