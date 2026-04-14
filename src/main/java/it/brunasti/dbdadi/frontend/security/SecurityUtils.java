package it.brunasti.dbdadi.frontend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(target::equals);
    }

    public static boolean isAdmin()     { return hasRole("ADMIN"); }
    public static boolean isPowerUser() { return hasRole("POWER_USER"); }
    public static boolean isEditor()    { return hasRole("EDITOR"); }
    public static boolean isViewer()    { return hasRole("VIEWER"); }

    /** True for ADMIN, POWER_USER, EDITOR — anyone who can create/edit/delete data */
    public static boolean canEdit() {
        return isAdmin() || isPowerUser() || isEditor();
    }

    /** True for ADMIN or POWER_USER — anyone who can run import/export */
    public static boolean canImportExport() {
        return isAdmin() || isPowerUser();
    }
}
