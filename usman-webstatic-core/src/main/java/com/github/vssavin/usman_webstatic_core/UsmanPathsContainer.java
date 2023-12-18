package com.github.vssavin.usman_webstatic_core;

import com.github.vssavin.usmancore.config.AuthorizedUrlPermission;
import com.github.vssavin.usmancore.config.Permission;
import com.github.vssavin.usmancore.config.PermissionPathsContainer;
import com.github.vssavin.usmancore.config.UsmanUrlsConfigurer;

import java.util.*;

/**
 * A {@link com.github.vssavin.usmancore.config.PermissionPathsContainer} implementation
 * with webstatic permission paths.
 *
 * @author vssavin on 16.12.2023.
 */
public class UsmanPathsContainer implements PermissionPathsContainer {

    private final Map<Permission, List<AuthorizedUrlPermission>> container = new EnumMap<>(Permission.class);

    private final UsmanUrlsConfigurer usmanUrlsConfigurer;

    public UsmanPathsContainer(UsmanUrlsConfigurer usmanUrlsConfigurer) {
        this.usmanUrlsConfigurer = usmanUrlsConfigurer;
        List<AuthorizedUrlPermission> anyUserPaths = getAnyUserPaths();
        List<AuthorizedUrlPermission> adminOnlyPaths = getAdminOnlyPaths();
        List<AuthorizedUrlPermission> userAdminPaths = getUserAdminPaths();
        container.put(Permission.ANY_USER, anyUserPaths);
        container.put(Permission.ADMIN_ONLY, adminOnlyPaths);
        container.put(Permission.USER_ADMIN, userAdminPaths);
    }

    @Override
    public List<AuthorizedUrlPermission> getPermissionPaths(Permission permission) {
        return container.get(permission);
    }

    private List<AuthorizedUrlPermission> getAnyUserPaths() {
        List<AuthorizedUrlPermission> paths = new ArrayList<>();
        paths.add(new AuthorizedUrlPermission("/js/**", Permission.ANY_USER));
        paths.add(new AuthorizedUrlPermission("/css/**", Permission.ANY_USER));
        paths.add(new AuthorizedUrlPermission("/usman/users/passwordRecovery", Permission.ANY_USER));
        paths.add(new AuthorizedUrlPermission("/usman/users/perform-password-recovery", HttpMethod.POST.name(),
                Permission.ANY_USER));

        paths.add(new AuthorizedUrlPermission(this.usmanUrlsConfigurer.getRegistrationUrl(), Permission.ANY_USER));

        paths.add(new AuthorizedUrlPermission(this.usmanUrlsConfigurer.getPerformRegisterUrl(), Permission.ANY_USER));

        paths.add(new AuthorizedUrlPermission("/usman/users/confirmUser", Permission.ANY_USER));

        paths.add(new AuthorizedUrlPermission(this.usmanUrlsConfigurer.getPerformRegisterUrl(), HttpMethod.POST.name(),
                Permission.ANY_USER));

        paths.add(new AuthorizedUrlPermission("/usman/security/key", Permission.ANY_USER));
        paths.add(new AuthorizedUrlPermission("/usman/users/perform-password-recovery", HttpMethod.POST.name(),
                Permission.ANY_USER));

        paths.add(new AuthorizedUrlPermission("/usman/languages", Permission.ANY_USER));
        paths.add(new AuthorizedUrlPermission("/flags/**", Permission.ANY_USER));
        paths.add(new AuthorizedUrlPermission("/img/**", Permission.ANY_USER));

        paths.add(new AuthorizedUrlPermission("/oauth/**", Permission.ANY_USER));
        paths.add(new AuthorizedUrlPermission("/login/oauth2/code/google", Permission.ANY_USER));

        paths.add(new AuthorizedUrlPermission(this.usmanUrlsConfigurer.getLoginUrl(), Permission.ANY_USER));

        return paths;
    }

    private List<AuthorizedUrlPermission> getAdminOnlyPaths() {
        List<AuthorizedUrlPermission> paths = new ArrayList<>();
        paths.add(new AuthorizedUrlPermission("/usman/admin**", Permission.ADMIN_ONLY));
        paths.add(new AuthorizedUrlPermission("/usman/admin/**", Permission.ADMIN_ONLY));
        paths.add(new AuthorizedUrlPermission("/usman/events/**", Permission.ADMIN_ONLY));
        paths.add(new AuthorizedUrlPermission(this.usmanUrlsConfigurer.getAdminUrl() + "/*", Permission.ADMIN_ONLY));

        paths.add(new AuthorizedUrlPermission(this.usmanUrlsConfigurer.getAdminUrl() + "/*", HttpMethod.PATCH.name(),
                Permission.ADMIN_ONLY));
        paths.add(new AuthorizedUrlPermission(this.usmanUrlsConfigurer.getAdminUrl() + "/*", HttpMethod.POST.name(),
                Permission.ADMIN_ONLY));
        paths.add(new AuthorizedUrlPermission(this.usmanUrlsConfigurer.getAdminUrl() + "/*", HttpMethod.DELETE.name(),
                Permission.ADMIN_ONLY));

        return paths;
    }

    private List<AuthorizedUrlPermission> getUserAdminPaths() {
        List<AuthorizedUrlPermission> paths = new ArrayList<>();
        paths.add(new AuthorizedUrlPermission("/usman/users/**", Permission.USER_ADMIN));
        paths.add(new AuthorizedUrlPermission(this.usmanUrlsConfigurer.getLogoutUrl(), Permission.USER_ADMIN));
        paths.add(new AuthorizedUrlPermission(this.usmanUrlsConfigurer.getPerformLogoutUrl(), Permission.USER_ADMIN));
        paths.add(new AuthorizedUrlPermission("/usman/users/changePassword", HttpMethod.PATCH.name(),
                Permission.USER_ADMIN));
        paths.add(new AuthorizedUrlPermission("/usman/users", HttpMethod.PATCH.name(), Permission.USER_ADMIN));
        paths.add(new AuthorizedUrlPermission("/*", Permission.USER_ADMIN));
        return paths;
    }

}
