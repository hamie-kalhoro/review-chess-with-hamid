package com.hamids.chessreviewerbyhamid.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/")
    public String home() {
        return "redirect:/chess";
    }

    @GetMapping("/chess")
    public String chessReviewer() {
        return "forward:/chess-index.html";
    }
}

