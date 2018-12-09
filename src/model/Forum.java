package model;

import javafx.geometry.Pos;

import java.util.List;

public class Forum {

    private List<Post> postList;


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


}
