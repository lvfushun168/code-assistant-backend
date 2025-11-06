package com.lfs.codeassistantbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.lfs.codeassistantbackend.repository")
public class CodeAssistantBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeAssistantBackendApplication.class, args);
    }

}
