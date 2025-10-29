package org.ulinda.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {
    @GetMapping("/helloWorld")
    public String sayHello() {
        return "Hello World!";
    }
}
