package com.walter.lifelog.controller;

import com.walter.lifelog.facade.PostFacade;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PostViewController {
    private final PostFacade postFacade;

    public PostViewController(PostFacade postFacade) {
        this.postFacade = postFacade;
    }

    @GetMapping("/post/editor")
    public String editor(Model model) {
        model.addAttribute("post", postFacade.getEditPost());
        return "editor";
    }

    @GetMapping("/post/editor/{postSeq}")
    public String editor(Model model, @PathVariable("postSeq") long postSeq) {
        model.addAttribute("post", postFacade.getEditPost(postSeq));
        return "editor";
    }
}
