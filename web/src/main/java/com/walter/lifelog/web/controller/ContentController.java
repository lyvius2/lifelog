package com.walter.lifelog.web.controller;

import com.walter.lifelog.content.service.ContentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import static com.walter.lifelog.content.entity.code.ContentType.CAR;
import static com.walter.lifelog.content.entity.code.ContentType.INTRO;
import static com.walter.lifelog.content.entity.code.ContentType.PROFILE;

@Controller
public class ContentController {
    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        model.addAttribute("content", contentService.getContentByType(INTRO));
        return "index";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("content", contentService.getContentByType(PROFILE));
        return "profile";
    }

    @GetMapping("/my-car")
    public String myCar(Model model) {
        model.addAttribute("content", contentService.getContentByType(CAR));
        return "my-car";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
