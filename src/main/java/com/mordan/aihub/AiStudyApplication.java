package com.mordan.aihub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

//@SpringBootApplication(exclude = {
//        dev.langchain4j.openai.spring.AutoConfig.class
//})
@SpringBootApplication
@EnableAsync
public class AiStudyApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiStudyApplication.class, args);
    }

}
