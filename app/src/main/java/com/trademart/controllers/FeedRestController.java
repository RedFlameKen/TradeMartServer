package com.trademart.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trademart.async.SharedResource;
import com.trademart.media.MediaController;
import com.trademart.post.PostController;
import com.trademart.service.ServiceController;
import com.trademart.user.UserController;

@RestController
public class FeedRestController {

    private SharedResource sharedResource;
    private UserController userController;
    private PostController postController;
    private ServiceController serviceController;

    private MediaController mediaController;

    public FeedRestController(SharedResource sharedResource){
        this.sharedResource = sharedResource;
        this.serviceController = new ServiceController(sharedResource);
        this.userController = new UserController(sharedResource);
        this.postController = new PostController(sharedResource);
        this.mediaController = new MediaController(sharedResource);
    }

    @GetMapping("/feed")
    public void serveFeedDetailsMapping(){
    }
    
}
