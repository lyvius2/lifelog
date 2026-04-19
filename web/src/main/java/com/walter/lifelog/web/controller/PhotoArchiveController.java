package com.walter.lifelog.web.controller;

import com.walter.lifelog.photo.facade.PhotoArchiveFacade;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PhotoArchiveController {
    private final PhotoArchiveFacade photoArchiveFacade;

    public PhotoArchiveController(PhotoArchiveFacade photoArchiveFacade) {
        this.photoArchiveFacade = photoArchiveFacade;
    }

    @GetMapping("/photos")
    public String photos(Model model, HttpServletRequest request) {
        var photoArchive = photoArchiveFacade.getPhotoArchiveViewInfo();
        model.addAttribute("categories", photoArchive.getCategories());
        model.addAttribute("archive", photoArchive.getArchive());
        model.addAttribute("period", photoArchive.getPeriod());
        HttpSession session = request.getSession(false);
        boolean isAdmin = session != null && session.getAttribute("userSeq") != null;
        model.addAttribute("isAdmin", isAdmin);
        return "photos";
    }

    @GetMapping("/photos/upload")
    public String photoUpload(Model model) {
        model.addAttribute("categories", photoArchiveFacade.getActivePhotoCategories());
        return "photo-upload";
    }
}
