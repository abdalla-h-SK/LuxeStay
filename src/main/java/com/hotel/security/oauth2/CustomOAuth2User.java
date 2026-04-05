package com.hotel.security.oauth2;

import com.hotel.security.UserPrincipal;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
public class CustomOAuth2User implements OAuth2User {

    private final UserPrincipal userPrincipal;
    private final Map<String, Object> attributes;

    public CustomOAuth2User(UserPrincipal userPrincipal, Map<String, Object> attributes) {
        this.userPrincipal = userPrincipal;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() { return attributes; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return userPrincipal.getAuthorities();
    }

    @Override
    public String getName() { return userPrincipal.getUsername(); }

    public Long getId() { return userPrincipal.getId(); }
    public String getEmail() { return userPrincipal.getEmail(); }
}
