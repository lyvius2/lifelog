package com.walter.lifelog.web.controller;

import com.walter.lifelog.photo.dto.PhotoSearchRequest;
import com.walter.lifelog.photo.service.PhotoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PhotoArchiveController {
    private final PhotoService photoService;

    public PhotoArchiveController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @GetMapping("/photos")
    public String photos(Model model) {
        model.addAttribute("categories", photoService.getActivePhotoCategories());
        model.addAttribute("archive", photoService.getPhotos(new PhotoSearchRequest()));
        return "photos";
    }

    @GetMapping("/photos/upload")
    public String photoUpload(Model model) {
        model.addAttribute("categories", photoService.getActivePhotoCategories());
        return "photo-upload";
    }
}
