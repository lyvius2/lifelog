package com.walter.lifelog.controller;

import com.walter.lifelog.service.PostService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PostViewController {
    private PostService postService;

    public PostViewController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/post/editor")
    public String editor(Model model) {
        model.addAttribute("post", postService.getEditPost());
        return "editor";
    }

    @GetMapping("/post/editor/{postSeq}")
    public String editor(Model model, @PathVariable("postSeq") long postSeq) {
        model.addAttribute("post", postService.getEditPost(postSeq));
        return "editor";
    }
}
