package com.github.vssavin.usman_webstatic.spring5.api.v1.user;

import com.github.vssavin.jcrypt.DefaultStringSafety;
import com.github.vssavin.jcrypt.StringSafety;
import com.github.vssavin.usman_webstatic.spring5.api.v1.Spring5WebstaticBaseController;
import com.github.vssavin.usman_webstatic_core.MessageKey;
import com.github.vssavin.usman_webstatic_core.UsmanLocaleConfig;
import com.github.vssavin.usmancore.config.*;
import com.github.vssavin.usmancore.email.EmailService;
import com.github.vssavin.usmancore.exception.user.EmailNotFoundException;
import com.github.vssavin.usmancore.exception.user.UserExistsException;
import com.github.vssavin.usmancore.exception.user.UserServiceException;
import com.github.vssavin.usmancore.security.SecureService;
import com.github.vssavin.usmancore.spring5.user.User;
import com.github.vssavin.usmancore.spring5.user.UserSecurityService;
import com.github.vssavin.usmancore.spring5.user.UserService;
import com.github.vssavin.usmancore.user.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static com.github.vssavin.usmancore.config.Role.ROLE_ADMIN;

/**
 * Provides user management for regular users.
 *
 * @author vssavin on 24.12.2023.
 */
@Controller
@RequestMapping(UserController.USER_CONTROLLER_PATH)
final class UserController extends Spring5WebstaticBaseController implements ArgumentsProcessedNotifier {

    static final String USER_CONTROLLER_PATH = "/usman/v1/users";

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private static final String PAGE_REGISTRATION = "registration";

    private static final String PAGE_CHANGE_PASSWORD = "changePassword";

    private static final String PAGE_CONFIRM_USER = "confirmUser";

    private static final String PAGE_USER_EDIT = "userEditYourself";

    private static final String PAGE_USER_CONTROL_PANEL = "userControlPanel";

    private static final String PERFORM_REGISTER_MAPPING = "/perform-register";

    private static final String PAGE_RECOVERY_PASSWORD = "passwordRecovery";

    private static final String PERFORM_PASSWORD_RECOVERY = "/perform-password-recovery";

    private static final String USER_UPDATE_ERROR_MSG = "User update error!";

    private static final Set<String> IGNORED_PARAMS = new HashSet<>();

    static {
        IGNORED_PARAMS.add("_csrf");
        IGNORED_PARAMS.add("newPassword");
        IGNORED_PARAMS.add("currentPassword");
    }

    private final Set<String> pageRegistrationParams;

    private final Set<String> pageChangePasswordParams;

    private final Set<String> pageConfirmUserParams;

    private final Set<String> pagePasswordRecoveryParams;

    private final Set<String> pageLoginParams;

    private final Set<String> pageUserEditParams;

    private final Set<String> pageUserControlPanelParams;

    private final UsmanConfigurer usmanConfigurer;

    private final UserService userService;

    private final UserSecurityService userSecurityService;

    private final EmailService emailService;

    private final PasswordEncoder passwordEncoder;

    private final StringSafety stringSafety = new DefaultStringSafety();

    private SecureService secureService;

    @Autowired
    UserController(UsmanConfigurer usmanConfigurer, UsmanUrlsConfigurer urlsConfigurer, UsmanLocaleConfig localeConfig,
            UserService userService, UserSecurityService userSecurityService, EmailService emailService,
            PasswordEncoder passwordEncoder) {
        super(urlsConfigurer);
        this.usmanConfigurer = usmanConfigurer;
        this.userService = userService;
        this.userSecurityService = userSecurityService;
        this.secureService = usmanConfigurer.getSecureService();
        this.emailService = emailService;
        String pageLogin = urlsConfigurer.getLoginUrl().replace("/", "");
        this.pageUserEditParams = localeConfig.forPage(PAGE_USER_EDIT).getKeys();
        this.pageUserControlPanelParams = localeConfig.forPage(PAGE_USER_CONTROL_PANEL).getKeys();
        this.pageLoginParams = localeConfig.forPage(pageLogin).getKeys();
        this.pageRegistrationParams = localeConfig.forPage(PAGE_REGISTRATION).getKeys();
        this.pageChangePasswordParams = localeConfig.forPage(PAGE_CHANGE_PASSWORD).getKeys();
        this.pageConfirmUserParams = localeConfig.forPage(PAGE_CONFIRM_USER).getKeys();
        this.pagePasswordRecoveryParams = localeConfig.forPage(PAGE_RECOVERY_PASSWORD).getKeys();
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void notifyArgumentsProcessed(Class<?> aClass) {
        if (aClass != null && UsmanSecureServiceArgumentsHandler.class.isAssignableFrom(aClass)) {
            this.secureService = usmanConfigurer.getSecureService();
        }
    }

    @GetMapping(value = { "/" + PAGE_REGISTRATION, "/" + PAGE_REGISTRATION + ".html" })
    ModelAndView registration(final HttpServletRequest request, final Model model,
            @RequestParam(required = false) final String lang) {
        ModelAndView modelAndView;

        if (!usmanConfigurer.isRegistrationAllowed()) {
            return getForbiddenModelAndView(request);
        }

        String authorizedName;
        try {
            authorizedName = userSecurityService.getAuthorizedUserName(request);
        }
        catch (UsernameNotFoundException e) {
            authorizedName = "";
        }

        if (!authorizedName.isEmpty()) {
            return getForbiddenModelAndView(request);
        }

        modelAndView = new ModelAndView(PAGE_REGISTRATION, model.asMap());
        modelAndView.addObject("userName", authorizedName);

        addObjectsToModelAndView(modelAndView, pageRegistrationParams, secureService.getEncryptMethodName(), lang);
        addObjectsToModelAndView(modelAndView, request.getParameterMap(), IGNORED_PARAMS);
        return modelAndView;
    }

    @PostMapping(PERFORM_REGISTER_MAPPING)
    ModelAndView performRegister(final HttpServletRequest request, final HttpServletResponse response,
            @RequestParam final String login, @RequestParam final String username, @RequestParam final String email,
            @RequestParam final String password, @RequestParam final String confirmPassword,
            @RequestParam(required = false) final String role, @RequestParam(required = false) final String lang) {
        ModelAndView modelAndView;
        if (!usmanConfigurer.isRegistrationAllowed()) {
            return getForbiddenModelAndView(request);
        }

        String authorizedName;
        try {
            authorizedName = userSecurityService.getAuthorizedUserName(request);
        }
        catch (UsernameNotFoundException e) {
            authorizedName = "";
        }

        if (!authorizedName.isEmpty()) {
            return getForbiddenModelAndView(request);
        }

        User newUser;
        Role registerRole;
        registerRole = Role.getRole(role);

        if (!userService.accessGrantedForRegistration(registerRole, authorizedName)) {
            modelAndView = getErrorModelAndView(PAGE_REGISTRATION, MessageKey.AUTHENTICATION_REQUIRED_MESSAGE, lang);
            addObjectsToModelAndView(modelAndView, pageRegistrationParams, secureService.getEncryptMethodName(), lang);

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return modelAndView;
        }

        boolean emailSendingFailed;
        try {
            if (!secureService.decrypt(password, secureService.getPrivateKey(request.getRemoteAddr()))
                .equals(secureService.decrypt(confirmPassword, secureService.getPrivateKey(request.getRemoteAddr())))) {
                modelAndView = getErrorModelAndView(PAGE_REGISTRATION, MessageKey.PASSWORDS_MUST_BE_IDENTICAL_MESSAGE,
                        lang);
                addObjectsToModelAndView(modelAndView, pageRegistrationParams, secureService.getEncryptMethodName(),
                        lang);
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                return modelAndView;
            }

            if (!isValidUserEmail(email)) {
                modelAndView = getErrorModelAndView(PAGE_REGISTRATION, MessageKey.EMAIL_NOT_VALID_MESSAGE, lang);
                addObjectsToModelAndView(modelAndView, pageRegistrationParams, secureService.getEncryptMethodName(),
                        lang);
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                return modelAndView;
            }

            String decodedPassword = secureService.decrypt(password,
                    secureService.getPrivateKey(request.getRemoteAddr()));

            if (!isValidUserPassword(usmanConfigurer.getPasswordPattern(), decodedPassword)) {
                modelAndView = new ModelAndView(REDIRECT_PREFIX + PAGE_REGISTRATION);
                modelAndView.addObject(ERROR_ATTRIBUTE, true);
                modelAndView.addObject(ERROR_MSG_ATTRIBUTE, usmanConfigurer.getPasswordPatternErrorMessage());
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                return modelAndView;
            }

            if (isEmailExist(email)) {
                modelAndView = getErrorModelAndView(PAGE_REGISTRATION, MessageKey.EMAIL_EXISTS_MESSAGE, lang);
                addObjectsToModelAndView(modelAndView, pageRegistrationParams, secureService.getEncryptMethodName(),
                        lang);
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                return modelAndView;
            }

            newUser = userService.registerUser(login, username, passwordEncoder.encode(decodedPassword), email,
                    registerRole);
            stringSafety.clearString(decodedPassword);
            String url = String.format("%s%s/%s?login=%s&verificationId=%s&lang=%s",
                    usmanConfigurer.getApplicationUrl(), USER_CONTROLLER_PATH, PAGE_CONFIRM_USER, login,
                    newUser.getVerificationId(), lang);

            emailSendingFailed = !sendEmail(email, url);

        }
        catch (UserExistsException e) {
            modelAndView = getErrorModelAndView(PAGE_REGISTRATION, MessageKey.USER_EXISTS_PATTERN, lang, username);
            addObjectsToModelAndView(modelAndView, pageRegistrationParams, secureService.getEncryptMethodName(), lang);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return modelAndView;
        }
        catch (Exception e) {

            modelAndView = getErrorModelAndView(PAGE_REGISTRATION, MessageKey.CREATE_USER_ERROR_MESSAGE, lang);
            addObjectsToModelAndView(modelAndView, pageRegistrationParams, secureService.getEncryptMethodName(), lang);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            log.error("Registration user error!", e);
            return modelAndView;
        }

        modelAndView = getSuccessModelAndView(PAGE_REGISTRATION, MessageKey.USER_CREATED_SUCCESSFULLY_PATTERN, lang,
                newUser.getLogin());
        modelAndView.addObject("emailSendingFailed", emailSendingFailed);

        addObjectsToModelAndView(modelAndView, pageRegistrationParams, secureService.getEncryptMethodName(), lang);

        return modelAndView;
    }

    @GetMapping(value = { "/" + PAGE_CHANGE_PASSWORD, "/" + PAGE_CHANGE_PASSWORD + ".html" },
            produces = { "application/json; charset=utf-8" })
    ModelAndView changeUserPassword(final HttpServletRequest request,
            @RequestParam(required = false) final String lang) {
        ModelAndView modelAndView = new ModelAndView(PAGE_CHANGE_PASSWORD);
        String authorizedName = "";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            authorizedName = authentication.getName();
        }
        if (!isAuthorizedUser(authorizedName)) {
            modelAndView = getErrorModelAndView(PAGE_CHANGE_PASSWORD, MessageKey.AUTHENTICATION_REQUIRED_MESSAGE, lang);
        }

        modelAndView.addObject(USER_NAME_ATTRIBUTE, authorizedName);
        addObjectsToModelAndView(modelAndView, pageChangePasswordParams, secureService.getEncryptMethodName(), lang);
        addObjectsToModelAndView(modelAndView, request.getParameterMap(), IGNORED_PARAMS);
        return modelAndView;
    }

    @PatchMapping(PAGE_CHANGE_PASSWORD)
    ModelAndView performChangeUserPassword(final HttpServletRequest request, final HttpServletResponse response,
            @RequestParam final String currentPassword, @RequestParam final String newPassword,
            @RequestParam(required = false) final String lang) {
        ModelAndView modelAndView;
        try {
            String authorizedUserName = "";
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                authorizedUserName = authentication.getName();
            }
            if (isAuthorizedUser(authorizedUserName)) {
                if (authorizedUserName.toLowerCase().contains("anonymoususer")) {
                    modelAndView = getErrorModelAndView(PAGE_CHANGE_PASSWORD,
                            MessageKey.AUTHENTICATION_REQUIRED_MESSAGE, lang);
                    addObjectsToModelAndView(modelAndView, pageChangePasswordParams,
                            secureService.getEncryptMethodName(), lang);
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    return modelAndView;
                }
                User user = userService.getUserByLogin(authorizedUserName);
                String realNewPassword = secureService.decrypt(newPassword,
                        secureService.getPrivateKey(request.getRemoteAddr()));
                String realCurrentPassword = secureService.decrypt(currentPassword,
                        secureService.getPrivateKey(request.getRemoteAddr()));
                if (user != null) {
                    if (passwordEncoder.matches(realCurrentPassword, user.getPassword())) {
                        user.setPassword(passwordEncoder.encode(realNewPassword));
                        userService.updateUser(user);
                    }
                    else {
                        modelAndView = getErrorModelAndView(PAGE_CHANGE_PASSWORD, MessageKey.WRONG_PASSWORD_MESSAGE,
                                lang);
                        response.setStatus(HttpStatus.BAD_REQUEST.value());
                        addObjectsToModelAndView(modelAndView, pageChangePasswordParams,
                                secureService.getEncryptMethodName(), lang);
                        return modelAndView;
                    }

                }
            }
            else {
                modelAndView = getErrorModelAndView(PAGE_CHANGE_PASSWORD, MessageKey.AUTHENTICATION_REQUIRED_MESSAGE,
                        lang);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                addObjectsToModelAndView(modelAndView, pageChangePasswordParams, secureService.getEncryptMethodName(),
                        lang);
                return modelAndView;
            }

        }
        catch (Exception ex) {
            modelAndView = getErrorModelAndView(PAGE_CHANGE_PASSWORD, MessageKey.REQUEST_PROCESSING_ERROR, lang);
            addObjectsToModelAndView(modelAndView, pageChangePasswordParams, secureService.getEncryptMethodName(),
                    lang);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return modelAndView;
        }

        modelAndView = getSuccessModelAndView(PAGE_CHANGE_PASSWORD, MessageKey.PASSWORD_SUCCESSFULLY_CHANGED_MESSAGE,
                lang);

        addObjectsToModelAndView(modelAndView, pageChangePasswordParams, secureService.getEncryptMethodName(), lang);
        addObjectsToModelAndView(modelAndView, request.getParameterMap(), IGNORED_PARAMS);

        return modelAndView;
    }

    @GetMapping(value = { "/" + PAGE_CONFIRM_USER, "/" + PAGE_CONFIRM_USER + ".html" })
    ModelAndView confirmUser(final HttpServletRequest request, final HttpServletResponse response,
            @RequestParam final String login, @RequestParam(required = false) String verificationId,
            @RequestParam(required = false) final String lang) {
        ModelAndView modelAndView;
        boolean isAdminUser = false;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            Collection<?> authorities = authentication.getAuthorities();
            for (Object authority : authorities) {
                if (authority.toString().equals(ROLE_ADMIN.name())) {
                    isAdminUser = true;
                }
            }
        }

        String resultMessage = UsmanLocaleConfig.getMessage(PAGE_CONFIRM_USER,
                MessageKey.CONFIRM_SUCCESS_MESSAGE.getKey(), lang);

        try {
            userService.confirmUser(login, verificationId, isAdminUser);
        }
        catch (Exception e) {
            resultMessage = UsmanLocaleConfig.getMessage(PAGE_CONFIRM_USER, MessageKey.CONFIRM_FAILED_MESSAGE.getKey(),
                    lang);
            log.error("User confirmation failed!", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        modelAndView = new ModelAndView(PAGE_CONFIRM_USER);
        modelAndView.addObject("confirmMessage", resultMessage);

        addObjectsToModelAndView(modelAndView, pageConfirmUserParams, secureService.getEncryptMethodName(), lang);
        addObjectsToModelAndView(modelAndView, request.getParameterMap(), IGNORED_PARAMS);
        return modelAndView;
    }

    @GetMapping(value = { "/" + PAGE_RECOVERY_PASSWORD, "/" + PAGE_RECOVERY_PASSWORD + ".html" })
    ModelAndView passwordRecovery(final HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "") final String recoveryId,
            @RequestParam(required = false) final String lang) {
        ModelAndView modelAndView = new ModelAndView(PAGE_RECOVERY_PASSWORD);
        boolean successSend = true;
        if (!recoveryId.isEmpty()) {
            try {
                User user = userService.getUserByRecoveryId(recoveryId);
                String newPassword = userService.generateNewUserPassword(recoveryId);
                String message = "Your new password: " + newPassword;
                emailService.sendSimpleMessage(user.getEmail(), "Your new password: ", message);
            }
            catch (UserServiceException usernameNotFoundException) {
                log.error("User not found! ", usernameNotFoundException);
                modelAndView.addObject("userNotFound", true);
                successSend = false;
            }
            catch (MailException mailException) {
                log.error("Failed to send an email!", mailException);
                modelAndView.addObject("failedSend", true);
                successSend = false;
            }
            modelAndView.addObject("successSend", successSend);
        }

        addObjectsToModelAndView(modelAndView, pagePasswordRecoveryParams, secureService.getEncryptMethodName(), lang);
        addObjectsToModelAndView(modelAndView, request.getParameterMap(), IGNORED_PARAMS);
        return modelAndView;
    }

    @PostMapping(PERFORM_PASSWORD_RECOVERY)
    ModelAndView performPasswordRecovery(final HttpServletRequest request, @RequestParam final String loginOrEmail,
            @RequestParam(required = false) final String lang) {
        ModelAndView modelAndView = new ModelAndView(REDIRECT_PREFIX + PAGE_RECOVERY_PASSWORD);
        boolean successSend = true;
        try {
            Map<String, User> map = userService.getUserRecoveryId(loginOrEmail);
            Optional<String> optionalRecoveryId = map.keySet().stream().findFirst();

            if (optionalRecoveryId.isPresent()) {
                User user = map.get(optionalRecoveryId.get());
                String message = usmanConfigurer.getApplicationUrl() + USER_CONTROLLER_PATH + "/"
                        + PAGE_RECOVERY_PASSWORD + "?recoveryId=" + optionalRecoveryId.get();
                emailService.sendSimpleMessage(user.getEmail(), "Password recovery", message);
            }
        }
        catch (UserServiceException usernameNotFoundException) {
            log.error("User not found: " + loginOrEmail + "! ", usernameNotFoundException);
            modelAndView.addObject("userNotFound", true);
            successSend = false;
        }
        catch (MailException mailException) {
            log.error("Failed to send an email!", mailException);
            modelAndView.addObject("failedSend", true);
            successSend = false;
        }

        modelAndView.addObject("successSend", successSend);

        addObjectsToModelAndView(modelAndView, pagePasswordRecoveryParams, secureService.getEncryptMethodName(), lang);
        addObjectsToModelAndView(modelAndView, request.getParameterMap(), IGNORED_PARAMS);
        return modelAndView;
    }

    @GetMapping(value = { "/" + PAGE_USER_EDIT + "/{login}", "/" + PAGE_USER_EDIT + ".html" + "/{login}" })
    ModelAndView userEdit(final HttpServletRequest request, final HttpServletResponse response,
            @PathVariable String login, @RequestParam(required = false) final boolean success,
            @RequestParam(required = false) final String successMsg,
            @RequestParam(required = false) final boolean error, @RequestParam(required = false) final String errorMsg,
            @RequestParam(required = false) final String lang) {

        String authorizedName;
        try {
            authorizedName = userSecurityService.getAuthorizedUserName(request);
        }
        catch (UsernameNotFoundException e) {
            authorizedName = "";
        }

        if (authorizedName.isEmpty()) {
            return getForbiddenModelAndView(request);
        }

        ModelAndView modelAndView = new ModelAndView(PAGE_USER_EDIT);
        User user;
        try {
            user = userService.getUserByLogin(login);

            if (!userSecurityService.getAuthorizedUserLogin(request).equals(user.getLogin())) {
                modelAndView = getErrorModelAndView(urlsConfigurer.getLoginUrl(),
                        MessageKey.ADMIN_AUTHENTICATION_REQUIRED_MESSAGE, lang);
                addObjectsToModelAndView(modelAndView, pageLoginParams, secureService.getEncryptMethodName(), lang);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                addObjectsToModelAndView(modelAndView, request.getParameterMap(), IGNORED_PARAMS);
                return modelAndView;
            }
        }
        catch (Exception e) {
            log.error(USER_UPDATE_ERROR_MSG, e);
            modelAndView = getErrorModelAndView(urlsConfigurer.getLoginUrl(), MessageKey.USER_EDIT_ERROR_MESSAGE, lang);
            addObjectsToModelAndView(modelAndView, pageUserEditParams, secureService.getEncryptMethodName(), lang);
            return modelAndView;
        }

        modelAndView.addObject("user", user);
        modelAndView.addObject(USER_NAME_ATTRIBUTE, authorizedName);
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

    @PatchMapping({ "", "/" })
    ModelAndView performUserEdit(final HttpServletRequest request, final HttpServletResponse response,
            @ModelAttribute final UserDto userDto, @RequestParam(required = false) final String lang) {

        ModelAndView modelAndView = new ModelAndView(PAGE_USER_EDIT);
        User newUser;
        try {
            User userFromDatabase = userService.getUserById(userDto.getId());

            if (!userSecurityService.getAuthorizedUserLogin(request).equals(userFromDatabase.getLogin())) {
                modelAndView = getErrorModelAndView(urlsConfigurer.getLoginUrl(),
                        MessageKey.ADMIN_AUTHENTICATION_REQUIRED_MESSAGE, lang);
                addObjectsToModelAndView(modelAndView, pageLoginParams, secureService.getEncryptMethodName(), lang);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                addObjectsToModelAndView(modelAndView, request.getParameterMap(), IGNORED_PARAMS);
                modelAndView.setViewName(modelAndView.getViewName() + "/" + userDto.getLogin());
                return modelAndView;
            }

            if (!isValidUserEmail(userDto.getEmail())) {
                modelAndView = getErrorModelAndView(PAGE_USER_EDIT, MessageKey.EMAIL_NOT_VALID_MESSAGE, lang);
                addObjectsToModelAndView(modelAndView, pageUserEditParams, secureService.getEncryptMethodName(), lang);
                modelAndView.setViewName(modelAndView.getViewName() + "/" + userFromDatabase.getLogin());
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                return modelAndView;
            }

            newUser = User.builder()
                .id(userFromDatabase.getId())
                .login(userFromDatabase.getLogin())
                .name(userDto.getName())
                .password(userFromDatabase.getPassword())
                .email(userDto.getEmail())
                .authority(userFromDatabase.getAuthority())
                .expirationDate(userFromDatabase.getExpirationDate())
                .verificationId(userFromDatabase.getVerificationId())
                .build();
            newUser = userService.updateUser(newUser);
            modelAndView.addObject("user", newUser);
            modelAndView.addObject(SUCCESS_ATTRIBUTE, true);
            String successMsg = UsmanLocaleConfig.getMessage("userEdit", MessageKey.USER_EDIT_SUCCESS_MESSAGE.getKey(),
                    lang);
            modelAndView.addObject(SUCCESS_MSG_ATTRIBUTE, successMsg);
        }
        catch (Exception e) {
            log.error(USER_UPDATE_ERROR_MSG, e);
            modelAndView = getErrorModelAndView(PAGE_USER_EDIT, MessageKey.USER_EDIT_ERROR_MESSAGE, lang);
            addObjectsToModelAndView(modelAndView, pageUserEditParams, secureService.getEncryptMethodName(), lang);
            return modelAndView;
        }

        modelAndView = new ModelAndView(
                REDIRECT_PREFIX + USER_CONTROLLER_PATH + "/" + PAGE_USER_EDIT + "/" + newUser.getLogin());

        addObjectsToModelAndView(modelAndView, PAGE_USER_EDIT, pageUserEditParams, secureService.getEncryptMethodName(),
                lang);
        addObjectsToModelAndView(modelAndView, request.getParameterMap(), IGNORED_PARAMS);

        String successMsg = UsmanLocaleConfig.getMessage(PAGE_USER_EDIT, MessageKey.USER_EDIT_SUCCESS_MESSAGE.getKey(),
                lang);
        modelAndView.addObject(SUCCESS_ATTRIBUTE, true);
        modelAndView.addObject(SUCCESS_MSG_ATTRIBUTE, successMsg);
        return modelAndView;
    }

    @GetMapping(value = { "/" + PAGE_USER_CONTROL_PANEL, "/" + PAGE_USER_CONTROL_PANEL + ".html" })
    ModelAndView userControlPanel(final HttpServletRequest request,
            @RequestParam(required = false) final boolean success,
            @RequestParam(required = false) final String successMsg,
            @RequestParam(required = false) final boolean error, @RequestParam(required = false) final String errorMsg,
            @RequestParam(required = false) final String lang) {

        String authorizedName;
        try {
            authorizedName = userSecurityService.getAuthorizedUserName(request);
        }
        catch (UsernameNotFoundException e) {
            authorizedName = "";
        }

        if (authorizedName.isEmpty()) {
            return getForbiddenModelAndView(request);
        }

        ModelAndView modelAndView = new ModelAndView(PAGE_USER_CONTROL_PANEL);
        User user;
        try {
            String login = userSecurityService.getAuthorizedUserLogin(request);
            user = userService.getUserByLogin(login);
        }
        catch (Exception e) {
            log.error(USER_UPDATE_ERROR_MSG, e);
            modelAndView = getErrorModelAndView(urlsConfigurer.getLoginUrl(), MessageKey.USER_EDIT_ERROR_MESSAGE, lang);
            addObjectsToModelAndView(modelAndView, pageUserControlPanelParams, secureService.getEncryptMethodName(),
                    lang);
            return modelAndView;
        }

        modelAndView.addObject("user", user);
        modelAndView.addObject("userEditUrl", USER_CONTROLLER_PATH + "/" + PAGE_USER_EDIT);
        modelAndView.addObject("changePasswordUrl", USER_CONTROLLER_PATH + "/" + PAGE_CHANGE_PASSWORD);
        modelAndView.addObject(USER_NAME_ATTRIBUTE, authorizedName);

        addObjectsToModelAndView(modelAndView, pageUserControlPanelParams, secureService.getEncryptMethodName(), lang);
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

    private boolean sendEmail(String email, String confirmUrl) {
        boolean emailSendingSuccessful = true;
        try {
            emailService.sendSimpleMessage(email,
                    String.format("User registration at %s", usmanConfigurer.getApplicationUrl()),
                    String.format("Confirm user registration: %s", confirmUrl));
        }
        catch (MailException mailException) {
            log.error("Sending email error!", mailException);
            emailSendingSuccessful = false;
        }

        return emailSendingSuccessful;
    }

}
