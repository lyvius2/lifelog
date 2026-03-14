package com.walter.lifelog.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SreViewController {

    @GetMapping("/sre")
    public String sre() {
        return "sre";
    }
}

