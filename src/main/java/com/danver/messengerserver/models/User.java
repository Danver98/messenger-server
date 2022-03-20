package com.danver.messengerserver.models;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonAlias;

public class User {
    private long id;
    private String email;
    //This field can store both raw password and hashed
    @JsonAlias({"password"})
    private String passwordHash;
    private byte[] salt;
    private String name;
    private String surname;
    private String avatarUrl;
    //private List<Chat> chats;

    public static class Builder {
        private final User user;

        public Builder() {
            user = new User();
        }

        public Builder id(long id) {
            user.id = id;
            return this;
        }

        public Builder email(String email) {
            user.email = email;
            return this;
        }

        public Builder password(String passwordHash) {
            user.passwordHash = passwordHash;
            return this;
        }

        public Builder salt(byte[] salt) {
            user.salt = salt;
            return this;
        }

        public Builder name(String name) {
            user.name = name;
            return this;
        }

        public Builder surname(String surname) {
            user.surname = surname;
            return this;
        }

        public Builder avatarUrl(String avatarUrl) {
            user.avatarUrl = avatarUrl;
            return this;
        }

        public User build() {
            return user;
        }
    }

    private User() {

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte [] salt) {
        this.salt = salt;
    }

    public User flushPasswordAndSalt() {
        this.passwordHash = null;
        this.salt = null;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id && email.equals(user.email) && passwordHash.equals(user.passwordHash) && name.equals(user.name) && surname.equals(user.surname) && Objects.equals(avatarUrl, user.avatarUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email, passwordHash, name, surname, avatarUrl);
    }
}
