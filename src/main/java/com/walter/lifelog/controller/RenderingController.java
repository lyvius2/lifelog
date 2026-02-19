package com.walter.lifelog.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RenderingController {
    @GetMapping({"/", "/index"})
    public String index() {
        return "index";
    }

    @GetMapping("/post")
    public String post() {
        return "post";
    }

    @GetMapping("/post/editor")
    public String editor() {
        return "editor";
    }

    @GetMapping("/photos")
    public String photos() {
        return "photos";
    }

    @GetMapping("/photos/upload")
    public String photoUpload() {
        return "photo-upload";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}
