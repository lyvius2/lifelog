package com.walter.lifelog.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/account")
public class AccountViewController {

    @GetMapping("/password")
    public String password() {
        return "account/password";
    }
}
