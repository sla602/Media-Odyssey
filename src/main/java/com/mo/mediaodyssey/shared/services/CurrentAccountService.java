package com.mo.mediaodyssey.shared.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import com.mo.mediaodyssey.auth.repository.UserRepository;
import com.mo.mediaodyssey.auth.security.MOOidcUserPrincipal;
import com.mo.mediaodyssey.shared.model.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class CurrentAccountService {

    @Autowired
    private UserRepository userRepository;

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

    public boolean isAuthenticated(Authentication authentication) {
        if (authentication == null) {
            return false;
        } else if (authentication instanceof AnonymousAuthenticationToken) {
            return false;
        } else {
            return authentication.isAuthenticated();
        }
    }

    private User resolveCurrentAccount(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Current visitor has not authenticated with a valid account.");
        } else {
            Object principal = authentication.getPrincipal();

            if (principal instanceof User user) {
                return user;
            }

            if (principal instanceof MOOidcUserPrincipal oidcPrincipal) {
                return oidcPrincipal.getUser();
            }

            throw new AuthenticationCredentialsNotFoundException(
                    "Current principal cannot be mapped to a valid account.");

        }
    }
}
