package com.walter.lifelog.web.controller;

import com.walter.lifelog.blog.dto.PostContents;
import com.walter.lifelog.blog.facade.PostFacade;
import com.walter.lifelog.shared.config.exception.PostNotFoundException;
import com.walter.lifelog.user.dto.Author;
import com.walter.lifelog.user.service.UserService;
import com.walter.lifelog.web.dto.PostView;
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PostViewController {
    private final PostFacade postFacade;
    private final UserService userService;

    public PostViewController(PostFacade postFacade, UserService userService) {
        this.postFacade = postFacade;
        this.userService = userService;
    }

    @GetMapping("/post/{inquiryStr}")
    public String post(@Parameter(description = "게시글 Seq 또는 slug", required = true) @PathVariable String inquiryStr,
                       Model model) {
        final PostContents postContents = postFacade.getPostContents(inquiryStr);
        if (ObjectUtils.anyNull(postContents, postContents.getWriterUserSeq())) {
            throw new PostNotFoundException(inquiryStr);
        }
        final Author author = userService.getAuthorInfoByUserSeq(postContents.getWriterUserSeq());
        model.addAttribute("post", PostView.of(postContents, author));
        return "post";
    }

    @GetMapping("/post/editor")
    public String editor(Model model) {
        model.addAttribute("post", postFacade.getPostEditorContents());
        return "editor";
    }

    @GetMapping("/post/editor/{postSeq}")
    public String editor(Model model, @PathVariable("postSeq") long postSeq) {
        model.addAttribute("post", postFacade.getPostEditorContents(postSeq));
        return "editor";
    }
}
