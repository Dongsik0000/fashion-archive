package com.fashion;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Scanner;

// 관리자 비밀번호의 BCrypt 해시를 만든다. 출력값을 환경변수 FASHION_ADMIN_PASSWORD_HASH에 넣는다.
public class PasswordHashTool {

    public static void main(String[] args) {
        System.out.print("비밀번호 입력: ");
        String raw = new Scanner(System.in).nextLine();
        System.out.println(new BCryptPasswordEncoder().encode(raw));
    }
}
