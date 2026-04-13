package com.walter.lifelog.web.controller;

import com.walter.lifelog.content.service.ContentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import static com.walter.lifelog.content.entity.code.ContentType.CAR;
import static com.walter.lifelog.content.entity.code.ContentType.INTRO;
import static com.walter.lifelog.content.entity.code.ContentType.PROFILE;
import static com.walter.lifelog.content.entity.code.ContentType.ARCHITECTURE;
import static com.walter.lifelog.content.entity.code.ContentType.AI_OPS_DESIGN;

@Controller
public class ContentViewController {
    private final ContentService contentService;

    public ContentViewController(ContentService contentService) {
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

    @GetMapping("/architecture")
    public String architecture(Model model) {
        model.addAttribute("content", contentService.getContentByType(ARCHITECTURE));
        return "architecture";
    }

    @GetMapping("/content/editor")
    public String contentEditor() {
        return "content-editor";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    @GetMapping("/ai-ops")
    public String aiOps() {
        return "ai-ops";
    }

    @GetMapping("/ai-ops/design")
    public String aiOpsDesign(Model model) {
        model.addAttribute("content", contentService.getContentByType(AI_OPS_DESIGN));
        return "ai-ops-design";
    }
}
