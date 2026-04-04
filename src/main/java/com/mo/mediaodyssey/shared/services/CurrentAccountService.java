package com.mo.mediaodyssey.shared.services;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import com.mo.mediaodyssey.auth.repository.UserRepository;
import com.mo.mediaodyssey.auth.security.MOOAuth2UserPrincipal;
import com.mo.mediaodyssey.shared.model.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class CurrentAccountService {

    private final UserRepository userRepository;

    public CurrentAccountService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return resolveCurrentAccount(authentication);
    }

    public User getCurrentAccount(Authentication authentication) {
        return resolveCurrentAccount(authentication);
    }

    public void refreshPrincipal(Authentication authentication, HttpServletRequest request,
            HttpServletResponse response) {
        User account = resolveCurrentAccount(authentication);
        Long accountId = account.getId();
        if (accountId == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Current principal cannot be refreshed because account id is missing.");
        }

        User refreshedAccount = userRepository.findById(accountId)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Current principal cannot be refreshed because account no longer exists."));

        UsernamePasswordAuthenticationToken refreshedToken = new UsernamePasswordAuthenticationToken(
                refreshedAccount,
                authentication.getCredentials(),
                refreshedAccount.getAuthorities());
        refreshedToken.setDetails(authentication.getDetails());

        SecurityContext refreshedContext = SecurityContextHolder.createEmptyContext();
        refreshedContext.setAuthentication(refreshedToken);
        SecurityContextHolder.setContext(refreshedContext);
        new HttpSessionSecurityContextRepository().saveContext(refreshedContext, request, response);
    }

    private User resolveCurrentAccount(Authentication authentication) {
        if (authentication == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Current visitor has not authenticated with a valid account.");
        }

        if (authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Current visitor has not authenticated with a valid account.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User user) {
            return user;
        }

        if (principal instanceof MOOAuth2UserPrincipal oauthPrincipal) {
            return oauthPrincipal.getUser();
        }

        throw new AuthenticationCredentialsNotFoundException(
                "Current principal cannot be mapped to a valid account.");
    }
}
