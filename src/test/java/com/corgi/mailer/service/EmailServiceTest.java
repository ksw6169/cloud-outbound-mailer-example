package com.corgi.mailer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EmailServiceTest {

    @Autowired
    private EmailService service;

    @BeforeEach
    void setUp() {
        this.service = new EmailService();
    }

    @Test
    void createTemplateMail() throws Exception {
        service.createTemplateMail();
    }

    @Test
    void deleteAddress() throws Exception {
        service.deleteAddress();
    }

    @Test
    void create() throws Exception {

        long startTime = System.currentTimeMillis();

        service.createMail();

//        for (int i=0; i<10; i++) {
//            service.createTestMail();
//        }

        long endTime = System.currentTimeMillis();

        System.out.println("endTime - startTime : " + (endTime-startTime));
    }

    @Test
    void createTemplate() throws Exception {

        long startTime = System.currentTimeMillis();

        for (int i=0; i<10; i++) {
            service.createTemplateMail();
        }

        long endTime = System.currentTimeMillis();

        System.out.println("endTime - startTime : " + (endTime-startTime));
    }
}