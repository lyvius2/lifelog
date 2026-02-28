package com.walter.lifelog.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PostViewController {
    @GetMapping("/post/editor")
    public String editor() {
        return "editor";
    }
}
