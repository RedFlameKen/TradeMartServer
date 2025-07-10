package com.trademart.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeedRestController {

    @GetMapping("/feed")
    public void serveFeedDetailsMapping(){
    }
    
}
