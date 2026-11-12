package com.javafx.nutrimaker.models;

public class User {
    private String email;
    private String password;
    private int id;

    private User() {}
    
    private void copyAll(User u){
        this.email = u.email;
        this.password = u.password;
        this.id = u.id;
    }
    
    public User(User u){
        copyAll(u);
    }

    public User(String e, String p,int i){
        this.email = e;
        this.password = p;
        this.id = i;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public int getId() {
        return id;
    }
    
}
