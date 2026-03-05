package com.walter.lifelog.web.controller;

import com.walter.lifelog.content.service.ContentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import static com.walter.lifelog.content.entity.code.ContentType.CAR;
import static com.walter.lifelog.content.entity.code.ContentType.PROFILE;

@Controller
public class ContentController {
    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("content", contentService.getContentByType(PROFILE));
        return "about";
    }

    @GetMapping("/my-car")
    public String myCar(Model model) {
        model.addAttribute("content", contentService.getContentByType(CAR));
        return "my-car";
    }
}
