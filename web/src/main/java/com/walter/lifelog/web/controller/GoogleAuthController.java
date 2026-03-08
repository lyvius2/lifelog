package com.walter.lifelog.web.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.walter.lifelog.shared.config.GoogleDriveConfig;
import com.walter.lifelog.web.util.RedirectUrlBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
@RequestMapping("/google-auth")
public class GoogleAuthController {
    private final GoogleAuthorizationCodeFlow flow;

    public GoogleAuthController(GoogleAuthorizationCodeFlow flow) {
        this.flow = flow;
    }

    @GetMapping({"", "/"})
    public String authorize(HttpServletRequest request) throws IOException {
        final String redirectUri = RedirectUrlBuilder.build(request);
        final var credential = flow.loadCredential(GoogleDriveConfig.USER_ID);
        if (credential != null && credential.getAccessToken() != null) {
            return "redirect:/google-auth/status";
        }
        final String authorizationUrl = flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .build();
        return "redirect:" + authorizationUrl;
    }

    @GetMapping("/callback")
    public String callback(@RequestParam("code") String code, HttpServletRequest request) throws IOException {
        final String redirectUri = RedirectUrlBuilder.build(request);
        final var tokenResponse = flow.newTokenRequest(code)
                .setRedirectUri(redirectUri)
                .execute();
        flow.createAndStoreCredential(tokenResponse, GoogleDriveConfig.USER_ID);
        return "google-auth/success";
    }

    @GetMapping("/status")
    public String status(Model model) throws IOException {
        final var credential = flow.loadCredential(GoogleDriveConfig.USER_ID);
        final boolean isAuthorized = credential != null && (credential.getAccessToken() != null || credential.getRefreshToken() != null);
        model.addAttribute("authenticated", isAuthorized);
        return "google-auth/status";
    }

    @GetMapping("/revoke")
    public String revoke(Model model) throws IOException {
        flow.getCredentialDataStore().delete(GoogleDriveConfig.USER_ID);
        return "redirect:/google-auth/status";
    }
}
