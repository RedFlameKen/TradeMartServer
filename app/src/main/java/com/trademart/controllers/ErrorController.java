package com.trademart.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// @RestController
public class ErrorController {

    public ErrorController(){
    }
    
    // @GetMapping("/error")
    public String error(){
        return "<span>there has been some kind of problem</span>";
    }

}
