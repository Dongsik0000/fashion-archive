package com.fashion.cmmn.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 화면 컨트롤러에서 던지면 404로 응답되고 web.xml의 error.jsp가 뜬다
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
