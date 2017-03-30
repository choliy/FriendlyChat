package com.choliy.igor.friendlychat;

class MessageModel {

    // Variable naming in model class must be the same as in FireBase.
    // Avoid using "m" & "s" prefix.
    private String message;
    private String user;
    private String photoUrl;

    // In FireBase model must be empty constructor,
    // for DataSnapshot (ChatActivity, 235 line).
    MessageModel() {
    }

    MessageModel(String message, String user, String photoUrl) {
        this.message = message;
        this.user = user;
        this.photoUrl = photoUrl;
    }

    String getMessage() {
        return message;
    }

    String getUser() {
        return user;
    }

    String getPhotoUrl() {
        return photoUrl;
    }
}