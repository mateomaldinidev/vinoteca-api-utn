package com.utn.vinoteca.features.auth;

import com.utn.vinoteca.features.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
public class CredentialsEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true,nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean enabled;

    @Column(name = "refresh_token", length = 2048)
    private String refreshToken;

    @OneToOne
    @JoinColumn(name = "usuario_id", referencedColumnName = "id", unique = true)
    private UserEntity usuario;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "credentials_roles",
            joinColumns = @JoinColumn(name = "credential_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        System.out.println("ROLES: " + roles);

        Set<GrantedAuthority> authorities = new HashSet<>();

        roles.forEach(role -> {

            System.out.println("ROL: " + role.getRole());
            System.out.println("PERMISOS: " + role.getPermits());

            authorities.add(
                    new SimpleGrantedAuthority(
                            role.getRole().name()
                    )
            );

            role.getPermits().forEach(permit ->
                    authorities.add(
                            new SimpleGrantedAuthority(
                                    permit.getPermit().name()
                            )
                    )
            );
        });

        System.out.println("AUTHORITIES FINALES: " + authorities);

        return authorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.enabled);
    }
}

