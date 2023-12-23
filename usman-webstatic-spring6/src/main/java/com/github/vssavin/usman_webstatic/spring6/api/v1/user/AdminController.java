package com.github.vssavin.usman_webstatic.spring6.api.v1.user;

import com.github.vssavin.jcrypt.DefaultStringSafety;
import com.github.vssavin.jcrypt.StringSafety;
import com.github.vssavin.usman_webstatic_core.MessageKey;
import com.github.vssavin.usman_webstatic_core.UsmanLocaleConfig;
import com.github.vssavin.usman_webstatic_core.UsmanWebstaticBaseController;
import com.github.vssavin.usmancore.config.*;
import com.github.vssavin.usmancore.data.pagination.Paged;
import com.github.vssavin.usmancore.exception.user.EmailNotFoundException;
import com.github.vssavin.usmancore.exception.user.UserExistsException;
import com.github.vssavin.usmancore.exception.user.UserNotFoundException;
import com.github.vssavin.usmancore.security.SecureService;
import com.github.vssavin.usmancore.spring6.user.User;
import com.github.vssavin.usmancore.spring6.user.UserMapper;
import com.github.vssavin.usmancore.spring6.user.UserSecurityService;
import com.github.vssavin.usmancore.spring6.user.UserService;
import com.github.vssavin.usmancore.user.UserDto;
import com.github.vssavin.usmancore.user.UserFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Provides user management for administrators.
 *
 * @author vssavin on 22.12.2023.
 */
@Controller
@RequestMapping(AdminController.ADMIN_CONTROLLER_PATH)
final class AdminController extends UsmanWebstaticBaseController implements ArgumentsProcessedNotifier {

    static final String ADMIN_CONTROLLER_PATH = "/usman/v1/admin";

    private static final String CONFIRM_USER_URL = "/usman/v1/users/confirmUser";

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private static final String PAGE_USERS = "users";

    private static final String PAGE_USER_EDIT = "edit";

    private static final String PAGE_EDIT = "userEdit";

    private static final String PAGE_ADMIN = "admin";

    private static final String PAGE_REGISTRATION = "registration";

    private static final String PAGE_CHANGE_USER_PASSWORD = "changeUserPassword";

    private static final String PAGE_CONFIRM_USER = "adminConfirmUser";

    private static final String PERFORM_REGISTER_MAPPING = "/perform-register";

    private static final Set<String> IGNORED_PARAMS = new HashSet<>();

    static {
        IGNORED_PARAMS.add("_csrf");
        IGNORED_PARAMS.add("newPassword");
    }

    private final Set<String> pageUsersParams;

    private final Set<String> pageUserEditParams;

    private final Set<String> pageLoginParams;

    private final Set<String> pageAdminParams;

    private final Set<String> pageRegistrationParams;

    private final Set<String> pageChangeUserPasswordParams;

    private final Set<String> pageAdminConfirmUserParams;

    private final UsmanConfigurer usmanConfigurer;

    private final UsmanUrlsConfigurer urlsConfigurer;

    private final UserService userService;

    private final UserSecurityService userSecurityService;

    private final PasswordEncoder passwordEncoder;

    private final UserMapper userMapper;

    private final StringSafety stringSafety = new DefaultStringSafety();

    private SecureService secureService;

    @Autowired
    AdminController(UsmanConfigurer usmanConfigurer, UsmanUrlsConfigurer urlsConfigurer, UsmanLocaleConfig localeConfig,
            UserService userService, UserSecurityService userSecurityService, PasswordEncoder passwordEncoder,
            UserMapper userMapper) {
        this.usmanConfigurer = usmanConfigurer;
        this.urlsConfigurer = urlsConfigurer;
        this.userService = userService;
        this.userSecurityService = userSecurityService;
        this.secureService = usmanConfigurer.getSecureService();
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        String pageLogin = urlsConfigurer.getLoginUrl().replace("/", "");
        pageLoginParams = localeConfig.forPage(pageLogin).getKeys();
        pageUsersParams = localeConfig.forPage(PAGE_USERS).getKeys();
        pageUserEditParams = localeConfig.forPage(PAGE_EDIT).getKeys();
        pageAdminParams = localeConfig.forPage(PAGE_ADMIN).getKeys();
        pageRegistrationParams = localeConfig.forPage(PAGE_REGISTRATION).getKeys();
        pageChangeUserPasswordParams = localeConfig.forPage(PAGE_CHANGE_USER_PASSWORD).getKeys();
        this.pageAdminConfirmUserParams = localeConfig.forPage(PAGE_CONFIRM_USER).getKeys();

    }

    @Override
    public void notifyArgumentsProcessed(Class<?> aClass) {
        if (aClass != null && UsmanSecureServiceArgumentsHandler.class.isAssignableFrom(aClass)) {
            this.secureService = usmanConfigurer.getSecureService();
        }
    }

    @GetMapping
    ModelAndView admin(final HttpServletResponse response, final HttpServletRequest request,
            @RequestParam(required = false) final String lang) {
        ModelAndView modelAndView;
        String authorizedName = userSecurityService.getAuthorizedUserName(request);
        if (isAuthorizedUser(authorizedName)) {
            modelAndView = new ModelAndView(PAGE_ADMIN);
            modelAndView.addObject(USER_NAME_ATTRIBUTE, authorizedName);
        }
        else {
            modelAndView = getErrorModelAndView(ERROR_PAGE, MessageKey.AUTHENTICATION_REQUIRED_MESSAGE, lang);
            response.setStatus(500);
        }

        addObjectsToModelAndView(modelAndView, pageAdminParams, secureService.getEncryptMethodName(), lang);

        return modelAndView;
    }

    @GetMapping(value = { "/" + PAGE_CONFIRM_USER, "/" + PAGE_CONFIRM_USER + ".html" })
    ModelAndView adminConfirmUser(final HttpServletResponse response, final HttpServletRequest request,
            @RequestParam(required = false) final String lang) {
        ModelAndView modelAndView;
        String authorizedName = userSecurityService.getAuthorizedUserName(request);
        if (isAuthorizedUser(authorizedName)) {
            modelAndView = new ModelAndView(PAGE_CONFIRM_USER);
            modelAndView.addObject(USER_NAME_ATTRIBUTE, authorizedName);
        }
        else {
            modelAndView = getErrorModelAndView(ERROR_PAGE, MessageKey.AUTHENTICATION_REQUIRED_MESSAGE, lang);
            response.setStatus(403);
        }

        addObjectsToModelAndView(modelAndView, pageAdminConfirmUserParams, secureService.getEncryptMethodName(), lang);
        modelAndView.addObject("confirmAction", CONFIRM_USER_URL);

        return modelAndView;
    }

    @GetMapping(value = { "/" + PAGE_REGISTRATION, "/" + PAGE_REGISTRATION + ".html" })
    ModelAndView registration(final HttpServletRequest request, final Model model,
            @RequestParam(required = false) final String lang) {
        ModelAndView modelAndView;
        String authorizedName = userSecurityService.getAuthorizedUserName(request);
        if (isAuthorizedUser(authorizedName)) {
            modelAndView = new ModelAndView(PAGE_REGISTRATION, model.asMap());
            modelAndView.addObject(USER_NAME_ATTRIBUTE, authorizedName);
            modelAndView.addObject("isAdmin", true);
        }
        else {
            modelAndView = new ModelAndView(ERROR_PAGE, model.asMap());

            modelAndView.addObject(ERROR_MSG_ATTRIBUTE, UsmanLocaleConfig.getMessage(PAGE_REGISTRATION,
                    MessageKey.AUTHENTICATION_REQUIRED_MESSAGE.getKey(), lang));
        }

        addObjectsToModelAndView(modelAndView, pageRegistrationParams, secureService.getEncryptMethodName(), lang);
        addObjectsToModelAndView(modelAndView, request.getParameterMap(), IGNORED_PARAMS);
        return modelAndView;

    }

    @PostMapping(PERFORM_REGISTER_MAPPING)
    ModelAndView performRegister(final HttpServletRequest request, final HttpServletResponse response,
            @RequestParam final String login, @RequestParam final String username, @RequestParam final String password,
            @RequestParam final String confirmPassword, @RequestParam final String email,
            @RequestParam(required = false) final String role, @RequestParam(required = false) final String lang) {
        ModelAndView modelAndView;

        User newUser;
        Role registerRole;
        registerRole = Role.getRole(role);

        if (!userSecurityService.isAuthorizedAdmin(request)) {
            modelAndView = getErrorModelAndView(PAGE_REGISTRATION, MessageKey.AUTHENTICATION_REQUIRED_MESSAGE, lang);
            addObjectsToModelAndView(modelAndView, pageRegistrationParams, secureService.getEncryptMethodName(), lang);
            response.setStatus(500);
            return modelAndView;
        }

        try {
            String key = secureService.getPrivateKey(request.getRemoteAddr());
            String decodedPassword = secureService.decrypt(password, key);
            String decodedConfirmPassword = secureService.decrypt(confirmPassword, key);
            if (!decodedPassword.equals(decodedConfirmPassword)) {
                modelAndView = getErrorModelAndView(PAGE_REGISTRATION, MessageKey.PASSWORDS_MUST_BE_IDENTICAL_MESSAGE,
                        lang);
                addObjectsToModelAndView(modelAndView, pageRegistrationParams, secureService.getEncryptMethodName(),
                        lang);
                response.setStatus(400);
                return modelAndView;
            }

            if (!isValidUserEmail(email)) {
                modelAndView = getErrorModelAndView(PAGE_REGISTRATION, MessageKey.EMAIL_NOT_VALID_MESSAGE, lang);
                addObjectsToModelAndView(modelAndView, pageRegistrationParams, secureService.getEncryptMethodName(),
                        lang);
                response.setStatus(400);
                return modelAndView;
            }

            if (!isValidUserPassword(usmanConfigurer.getPasswordPattern(), decodedPassword)) {
                modelAndView = new ModelAndView("redirect:" + PAGE_REGISTRATION);
                modelAndView.addObject(ERROR_ATTRIBUTE, true);
                modelAndView.addObject(ERROR_MSG_ATTRIBUTE, usmanConfigurer.getPasswordPatternErrorMessage());
                response.setStatus(400);
                return modelAndView;
            }

            if (isEmailExist(email)) {
                modelAndView = getErrorModelAndView(PAGE_REGISTRATION, MessageKey.EMAIL_EXISTS_MESSAGE, lang);
                addObjectsToModelAndView(modelAndView, pageRegistrationParams, secureService.getEncryptMethodName(),
                        lang);
                response.setStatus(400);
                return modelAndView;
            }

            newUser = userService.registerUser(login, username, passwordEncoder.encode(decodedPassword), email,
                    registerRole);
            stringSafety.clearString(decodedPassword);
            stringSafety.clearString(decodedConfirmPassword);
            userService.confirmUser(login, "", true);
        }
        catch (UserExistsException e) {
            log.error("User exists! ", e);
            modelAndView = getErrorModelAndView(PAGE_REGISTRATION, MessageKey.USER_EXISTS_PATTERN, lang, username);
            addObjectsToModelAndView(modelAndView, pageRegistrationParams, secureService.getEncryptMethodName(), lang);
            response.setStatus(400);
            return modelAndView;
        }
        catch (Exception e) {
            log.error("User registration error! ", e);
            modelAndView = getErrorModelAndView(PAGE_REGISTRATION, MessageKey.CREATE_USER_ERROR_MESSAGE, lang);
            addObjectsToModelAndView(modelAndView, pageRegistrationParams, secureService.getEncryptMethodName(), lang);
            response.setStatus(500);
            return modelAndView;
        }

        modelAndView = getSuccessModelAndView(PAGE_REGISTRATION, MessageKey.USER_CREATED_SUCCESSFULLY_PATTERN, lang,
                newUser.getLogin());
        addObjectsToModelAndView(modelAndView, pageRegistrationParams, secureService.getEncryptMethodName(), lang);

        return modelAndView;
    }

    @GetMapping(value = { "/" + PAGE_CHANGE_USER_PASSWORD, "/" + PAGE_CHANGE_USER_PASSWORD + ".html" },
            produces = { "application/json; charset=utf-8" })
    ModelAndView changeUserPassword(final HttpServletRequest request,
            @RequestParam(required = false) final String lang) {
        ModelAndView modelAndView = new ModelAndView(PAGE_CHANGE_USER_PASSWORD);
        String authorizedName = userSecurityService.getAuthorizedUserName(request);
        if (!isAuthorizedUser(authorizedName)) {
            modelAndView = getErrorModelAndView(ERROR_PAGE, MessageKey.AUTHENTICATION_REQUIRED_MESSAGE, lang);
        }

        modelAndView.addObject(USER_NAME_ATTRIBUTE, authorizedName);
        addObjectsToModelAndView(modelAndView, pageChangeUserPasswordParams, secureService.getEncryptMethodName(),
                lang);
        addObjectsToModelAndView(modelAndView, request.getParameterMap(), IGNORED_PARAMS);
        return modelAndView;
    }

    @PatchMapping(PAGE_CHANGE_USER_PASSWORD)
    ModelAndView performChangeUserPassword(final HttpServletRequest request, final HttpServletResponse response,
            @RequestParam String userName, @RequestParam String newPassword,
            @RequestParam(required = false) final String lang) {
        ModelAndView modelAndView;
        try {
            String authorizedUserName = userSecurityService.getAuthorizedUserName(request);
            if (isAuthorizedUser(authorizedUserName)) {
                User user = userService.getUserByLogin(userName);
                String realNewPassword = secureService.decrypt(newPassword,
                        secureService.getPrivateKey(request.getRemoteAddr()));
                if (user != null) {
                    user.setPassword(passwordEncoder.encode(realNewPassword));
                    stringSafety.clearString(realNewPassword);
                    userService.updateUser(user);
                }
                else {
                    modelAndView = getErrorModelAndView(PAGE_CHANGE_USER_PASSWORD, MessageKey.USER_NOT_FOUND_MESSAGE,
                            lang);
                    addObjectsToModelAndView(modelAndView, pageChangeUserPasswordParams,
                            secureService.getEncryptMethodName(), lang);
                    response.setStatus(404);
                    return modelAndView;
                }
            }
            else {
                modelAndView = getErrorModelAndView(PAGE_CHANGE_USER_PASSWORD,
                        MessageKey.AUTHENTICATION_REQUIRED_MESSAGE, lang);
                addObjectsToModelAndView(modelAndView, pageChangeUserPasswordParams,
                        secureService.getEncryptMethodName(), lang);
                response.setStatus(403);
                return modelAndView;
            }

        }
        catch (UsernameNotFoundException ex) {
            log.error("User name not found!", ex);
            modelAndView = getErrorModelAndView(PAGE_CHANGE_USER_PASSWORD, MessageKey.USER_NOT_FOUND_MESSAGE, lang);
            addObjectsToModelAndView(modelAndView, pageChangeUserPasswordParams, secureService.getEncryptMethodName(),
                    lang);
            response.setStatus(404);
            return modelAndView;
        }
        catch (Exception ex) {
            log.error("User password change error! ", ex);
            modelAndView = getErrorModelAndView(PAGE_CHANGE_USER_PASSWORD, MessageKey.REQUEST_PROCESSING_ERROR, lang);
            addObjectsToModelAndView(modelAndView, pageChangeUserPasswordParams, secureService.getEncryptMethodName(),
                    lang);
            response.setStatus(500);
            return modelAndView;
        }

        modelAndView = getSuccessModelAndView(PAGE_CHANGE_USER_PASSWORD,
                MessageKey.PASSWORD_SUCCESSFULLY_CHANGED_MESSAGE, lang);
        response.setStatus(200);
        addObjectsToModelAndView(modelAndView, pageChangeUserPasswordParams, secureService.getEncryptMethodName(),
                lang);
        addObjectsToModelAndView(modelAndView, request.getParameterMap(), IGNORED_PARAMS);

        return modelAndView;
    }

    @GetMapping(value = { "/" + PAGE_USERS, "/" + PAGE_USERS + ".html" })
    ModelAndView users(final HttpServletRequest request, final HttpServletResponse response,
            @ModelAttribute final UserFilter userFilter,
            @RequestParam(required = false, defaultValue = "1") final int page,
            @RequestParam(required = false, defaultValue = "5") final int size,
            @RequestParam(required = false) final String lang) {

        ModelAndView modelAndView = new ModelAndView(PAGE_USERS);
        if (userSecurityService.isAuthorizedAdmin(request)) {
            Paged<User> users = userService.getUsers(userFilter, page, size);
            modelAndView.addObject(USERS_ATTRIBUTE, users);
            modelAndView.addObject(USER_NAME_ATTRIBUTE, userSecurityService.getAuthorizedUserName(request));
        }
        else {
            modelAndView = getErrorModelAndView(urlsConfigurer.getLoginUrl(),
                    MessageKey.ADMIN_AUTHENTICATION_REQUIRED_MESSAGE, lang);
            addObjectsToModelAndView(modelAndView, pageLoginParams, secureService.getEncryptMethodName(), lang);
            response.setStatus(403);
        }

        addObjectsToModelAndView(modelAndView, pageUsersParams, secureService.getEncryptMethodName(), lang);
        addObjectsToModelAndView(modelAndView, request.getParameterMap(), IGNORED_PARAMS);

        return modelAndView;
    }

    @GetMapping(value = { "/" + PAGE_USERS + "/" + PAGE_USER_EDIT + "/{id}",
            "/" + PAGE_USERS + "/" + PAGE_USER_EDIT + ".html" + "/{id}" })
    ModelAndView userEdit(final HttpServletRequest request, final HttpServletResponse response,
            @PathVariable final Long id, @RequestParam(required = false) final boolean success,
            @RequestParam(required = false) final String successMsg,
            @RequestParam(required = false) final boolean error, @RequestParam(required = false) final String errorMsg,
            @RequestParam(required = false) final String lang) {

        ModelAndView modelAndView = new ModelAndView(PAGE_EDIT);
        if (userSecurityService.isAuthorizedAdmin(request)) {
            User user = userService.getUserById(id);
            UserDto userDto = userMapper.toDto(user);
            modelAndView.addObject(USER_ATTRIBUTE, userDto);
        }
        else {
            modelAndView = getErrorModelAndView(urlsConfigurer.getLoginUrl(),
                    MessageKey.ADMIN_AUTHENTICATION_REQUIRED_MESSAGE, lang);
            addObjectsToModelAndView(modelAndView, pageLoginParams, secureService.getEncryptMethodName(), lang);
            response.setStatus(403);
            addObjectsToModelAndView(modelAndView, request.getParameterMap(), IGNORED_PARAMS);
            return modelAndView;
        }

        addObjectsToModelAndView(modelAndView, pageUserEditParams, secureService.getEncryptMethodName(), lang);
        addObjectsToModelAndView(modelAndView, request.getParameterMap(), IGNORED_PARAMS);

        if (successMsg != null) {
            modelAndView.addObject(SUCCESS_ATTRIBUTE, success);
            modelAndView.addObject(SUCCESS_MSG_ATTRIBUTE, successMsg);
        }

        if (errorMsg != null) {
            modelAndView.addObject(ERROR_ATTRIBUTE, error);
            modelAndView.addObject(ERROR_MSG_ATTRIBUTE, errorMsg);
        }

        return modelAndView;
    }

    @PatchMapping(PAGE_USERS)
    ModelAndView performUserEdit(final HttpServletRequest request, final HttpServletResponse response,
            @ModelAttribute final UserDto userDto, @RequestParam(required = false) final String lang) {
        ModelAndView modelAndView = new ModelAndView(PAGE_EDIT);
        try {
            if (userSecurityService.isAuthorizedAdmin(request)) {
                if (!isValidUserEmail(userDto.getEmail())) {
                    modelAndView = getErrorModelAndView(PAGE_USERS, MessageKey.EMAIL_NOT_VALID_MESSAGE, lang);
                    addObjectsToModelAndView(modelAndView, pageUsersParams, secureService.getEncryptMethodName(), lang);
                    response.setStatus(400);
                    return modelAndView;
                }

                if (!isLoginValid(userDto.getLogin(), userDto.getId())) {
                    modelAndView = getErrorModelAndView(PAGE_USERS, MessageKey.USER_EXISTS_PATTERN, lang,
                            userDto.getLogin());
                    addObjectsToModelAndView(modelAndView, pageUsersParams, secureService.getEncryptMethodName(), lang);
                    response.setStatus(400);
                    return modelAndView;
                }

                User userFromDatabase = userService.getUserById(userDto.getId());
                User newUser = User.builder()
                    .id(userFromDatabase.getId())
                    .login(userDto.getLogin())
                    .name(userDto.getName())
                    .password(userFromDatabase.getPassword())
                    .email(userDto.getEmail())
                    .accountLocked(userDto.isAccountLocked())
                    .credentialsExpired(userDto.isCredentialsExpired())
                    .enabled(userDto.isEnabled())
                    .authority(userFromDatabase.getAuthority())
                    .expirationDate(userFromDatabase.getExpirationDate())
                    .verificationId(userFromDatabase.getVerificationId())
                    .build();
                newUser = userService.updateUser(newUser);
                modelAndView.addObject("user", newUser);
                modelAndView.addObject(SUCCESS_ATTRIBUTE, true);
                String successMsg = UsmanLocaleConfig.getMessage(PAGE_EDIT,
                        MessageKey.USER_EDIT_SUCCESS_MESSAGE.getKey(), lang);
                modelAndView.addObject(SUCCESS_MSG_ATTRIBUTE, successMsg);
            }
            else {
                modelAndView = getErrorModelAndView(urlsConfigurer.getLoginUrl(),
                        MessageKey.ADMIN_AUTHENTICATION_REQUIRED_MESSAGE, lang);
                addObjectsToModelAndView(modelAndView, pageLoginParams, secureService.getEncryptMethodName(), lang);
                response.setStatus(403);
                return modelAndView;
            }
        }
        catch (Exception e) {
            log.error("User update error! ", e);
            modelAndView = getErrorModelAndView(urlsConfigurer.getLoginUrl(), MessageKey.USER_EDIT_ERROR_MESSAGE, lang);
            addObjectsToModelAndView(modelAndView, pageUserEditParams, secureService.getEncryptMethodName(), lang);
            return modelAndView;
        }

        modelAndView = new ModelAndView(
                "redirect:" + ADMIN_CONTROLLER_PATH + "/" + PAGE_USERS + "/" + PAGE_USER_EDIT + "/" + userDto.getId());

        addObjectsToModelAndView(modelAndView, PAGE_EDIT, pageUserEditParams, secureService.getEncryptMethodName(),
                lang);
        addObjectsToModelAndView(modelAndView, request.getParameterMap(), IGNORED_PARAMS);

        String successMsg = UsmanLocaleConfig.getMessage(PAGE_EDIT, MessageKey.USER_EDIT_SUCCESS_MESSAGE.getKey(),
                lang);
        modelAndView.addObject(SUCCESS_ATTRIBUTE, true);
        modelAndView.addObject(SUCCESS_MSG_ATTRIBUTE, successMsg);
        return modelAndView;
    }

    @DeleteMapping(PAGE_USERS)
    ModelAndView deleteUser(final HttpServletRequest request, final HttpServletResponse response,
            @RequestParam final Long id, @RequestParam(required = false, defaultValue = "1") final int page,
            @RequestParam(required = false, defaultValue = "5") final int size,
            @RequestParam(required = false) final String lang) {
        ModelAndView modelAndView = new ModelAndView(PAGE_USERS);
        if (userSecurityService.isAuthorizedAdmin(request)) {
            try {
                modelAndView.addObject(USER_NAME_ATTRIBUTE, userSecurityService.getAuthorizedUserName(request));
                User user = userService.getUserById(id);
                if (user.getLogin().isEmpty()) {
                    String errorMessage = UsmanLocaleConfig.getMessage(PAGE_USERS,
                            MessageKey.USER_DELETE_ERROR_MESSAGE.getKey(), lang);
                    modelAndView.addObject(ERROR_ATTRIBUTE, true);
                    modelAndView.addObject(ERROR_MSG_ATTRIBUTE, errorMessage);
                }
                else {
                    userService.deleteUser(user);
                }
            }
            catch (UserNotFoundException e) {
                String errorMessage = UsmanLocaleConfig.getMessage(PAGE_USERS,
                        MessageKey.USER_DELETE_ERROR_MESSAGE.getKey(), lang);
                modelAndView.addObject(ERROR_ATTRIBUTE, true);
                modelAndView.addObject(ERROR_MSG_ATTRIBUTE, errorMessage);
                response.setStatus(404);
            }
        }
        else {
            modelAndView = getErrorModelAndView(urlsConfigurer.getLoginUrl(),
                    MessageKey.ADMIN_AUTHENTICATION_REQUIRED_MESSAGE, lang);
            addObjectsToModelAndView(modelAndView, pageLoginParams, secureService.getEncryptMethodName(), lang);
            response.setStatus(403);
            return modelAndView;
        }

        Paged<User> users = userService.getUsers(UserFilter.emptyUserFilter(), page, size);
        modelAndView.addObject(USERS_ATTRIBUTE, users);

        addObjectsToModelAndView(modelAndView, pageUsersParams, secureService.getEncryptMethodName(), lang);
        addObjectsToModelAndView(modelAndView, request.getParameterMap(), IGNORED_PARAMS);

        return modelAndView;
    }

    private boolean isEmailExist(String email) {
        boolean emailExist = false;
        try {
            userService.getUserByEmail(email);
            emailExist = true;
        }
        catch (EmailNotFoundException ignored) { // ignore
        }

        return emailExist;
    }

    private boolean isLoginValid(String login, Long id) {
        // login is valid if user not found or user.id equals id
        boolean loginValid = true;
        try {
            User user = userService.getUserByLogin(login);
            if (!Objects.equals(user.getId(), id)) {
                loginValid = false;
            }
        }
        catch (UsernameNotFoundException ignore) { // ignore
        }

        return loginValid;
    }

}
