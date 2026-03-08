package com.walter.lifelog.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RenderingController {
    @GetMapping("/photos")
    public String photos() {
        return "photos";
    }

    @GetMapping("/photos/upload")
    public String photoUpload() {
        return "photo-upload";
    }
}
