package com.walter.lifelog.web.controller;

import com.walter.lifelog.blog.dto.PostContents;
import com.walter.lifelog.blog.dto.PostSearchCondition;
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
import org.springframework.web.bind.annotation.RequestParam;

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

    @GetMapping("/post-list")
    public String postList() {
        return "redirect:/post-list/1";
    }

    @GetMapping("/post-list/{page}")
    public String postList(Model model, @Parameter(description = "페이지 번호 (1부터 시작)", required = true) @PathVariable("page") int page,
                           @Parameter(description = "검색 키워드") @RequestParam(value = "keyword", required = false) String keyword,
                           @Parameter(description = "카테고리") @RequestParam(value = "categorySeq", required = false) Long categorySeq,
                           @Parameter(description = "태그") @RequestParam(value = "tag", required = false) String tag) {
        var condition = PostSearchCondition.of(keyword, categorySeq, tag, page);
        model.addAttribute("postPage", postFacade.getSearchedPosts(condition));
        return "post-list";
    }
}
