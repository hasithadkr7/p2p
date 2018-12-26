package com.dc.peer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Forum {

    @JsonProperty("posts")
    private List<Post> postList;

    public Forum() {
        this.postList = new ArrayList<>();
    }

    public List<Post> getPostList() {
        return postList;
    }

    public void setPostList(List<Post> postList) {
        this.postList = postList;
    }


    // other methods.
    public Post getPostBytId(int postId) {
       return this.postList.stream().filter(post -> postId == post.getPostId()).findFirst().get();
    }

    public boolean postExist(Post post) {
        return postList.stream().anyMatch(post1 -> post.getPostId() == post1.getPostId()) ;
    }

    public void updatePost(Post post) {
        int index = postList.indexOf(getPostBytId(post.getPostId()));
        postList.remove(index);
        postList.add(index, post);
    }

    public void addPost(Post post) {
        postList.add(post);
    }

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
