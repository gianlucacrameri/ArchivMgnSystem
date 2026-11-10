package com.archiv.ArchivMgnSystem.security;


import com.archiv.ArchivMgnSystem.enums.UserRole;
import com.archiv.ArchivMgnSystem.models.User;
import lombok.Builder;
import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
@Builder
public class AuthUser implements UserDetails {
    private User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(user.getRole().name()));
    }

    @Override
    public @Nullable String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Noch nicht freigegebene Konten haben keinen Zugriff. Admins sind immer
        // aktiv, damit der erste Admin weiterhin per SQL ernannt werden kann.
        return user.isApproved() || user.getRole() == UserRole.ADMIN;
    }
}
