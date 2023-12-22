package com.github.vssavin.usman_webstatic.spring5.user;

import com.github.vssavin.usman_webstatic.spring5.AbstractTest;
import com.github.vssavin.usman_webstatic_core.MessageKey;
import com.github.vssavin.usman_webstatic_core.UsmanLocaleConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.ModelAndView;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author vssavin on 22.12.2023.
 */
public class AdminControllerTest extends AbstractTest {

    private static final String BASE_URL = "/usman/v1/admin";

    private UsmanLocaleConfig.LocaleSpringMessageSource registrationMessageSource;

    private UsmanLocaleConfig.LocaleSpringMessageSource changeUserPasswordMessageSource;

    @Autowired
    public void setRegistrationMessageSource(UsmanLocaleConfig.LocaleSpringMessageSource registrationMessageSource) {
        this.registrationMessageSource = registrationMessageSource;
    }

    @Autowired
    public void setChangeUserPasswordMessageSource(
            UsmanLocaleConfig.LocaleSpringMessageSource changeUserPasswordMessageSource) {
        this.changeUserPasswordMessageSource = changeUserPasswordMessageSource;
    }

    @Test
    public void registerUserSuccessful() throws Exception {
        MultiValueMap<String, String> registerParams = new LinkedMultiValueMap<>();
        String encodedPassword = encrypt("", "user2");

        String login = "user2";
        registerParams.add("login", login);
        registerParams.add("username", login);
        registerParams.add("email", "user2@example.com");
        registerParams.add("password", encodedPassword);
        registerParams.add("confirmPassword", encodedPassword);
        ResultActions resultActions = mockMvc.perform(post(BASE_URL + "/perform-register").params(registerParams)
            .with(getRequestPostProcessorForUser(testAdminUser()))
            .with(csrf()));
        String messagePattern = registrationMessageSource.getMessage(
                MessageKey.USER_CREATED_SUCCESSFULLY_PATTERN.getKey(), new Object[] {},
                UsmanLocaleConfig.DEFAULT_LOCALE);
        resultActions.andExpect(model().attribute("success", true))
            .andExpect(model().attribute("successMsg", String.format(messagePattern, login)))
            .andExpect(status().is(302));
    }

    @Test
    public void changeUserPasswordSuccessful() throws Exception {
        String previousPassword = testAdminUser().getPassword();
        String newPassword = "admin2";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        String encodedPassword = encrypt("", newPassword);

        params.add("userName", testAdminUser().getLogin());
        params.add("newPassword", encodedPassword);

        ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/changeUserPassword").params(params)
            .with(getRequestPostProcessorForUser(testAdminUser()))
            .with(csrf()));
        String message = changeUserPasswordMessageSource.getMessage(
                MessageKey.PASSWORD_SUCCESSFULLY_CHANGED_MESSAGE.getKey(), new Object[] {},
                UsmanLocaleConfig.DEFAULT_LOCALE);
        resultActions.andExpect(model().attribute("success", true)).andExpect(model().attribute("successMsg", message));

        params.clear();
        encodedPassword = encrypt("", previousPassword);
        params.add("userName", testAdminUser().getLogin());
        params.add("newPassword", encodedPassword);
        resultActions = mockMvc.perform(patch(BASE_URL + "/changeUserPassword").params(params)
            .with(getRequestPostProcessorForUser(testAdminUser()))
            .with(csrf()));
        resultActions.andExpect(model().attribute("success", true)).andExpect(model().attribute("successMsg", message));
    }

    @Test
    public void changeUserPasswordFailedUserNotFound() throws Exception {
        String userName = "UserNotFoundName";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        params.add("userName", userName);
        params.add("newPassword", encrypt("", testAdminUser().getPassword()));

        ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/changeUserPassword").params(params)
            .with(getRequestPostProcessorForUser(testAdminUser()))
            .with(csrf()));
        String message = changeUserPasswordMessageSource.getMessage(MessageKey.USER_NOT_FOUND_MESSAGE.getKey(),
                new Object[] {}, UsmanLocaleConfig.DEFAULT_LOCALE);
        resultActions.andExpect(model().attribute("error", true)).andExpect(model().attribute("errorMsg", message));
    }

    @Test
    public void userFilteringTest() throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("login", testAdminUser().getLogin());

        ResultActions resultActions = mockMvc.perform(get(BASE_URL + "/users").params(params)
            .with(getRequestPostProcessorForUser(testAdminUser()))
            .with(csrf()));

        String html = resultActions.andReturn().getResponse().getContentAsString();
        Document doc = Jsoup.parse(html);
        Element usersTable = doc.getElementById("usersTable");
        Elements trElements = usersTable.getElementsByTag("tbody").first().getElementsByTag("tr");
        Assertions.assertEquals(1, trElements.size());

    }

    @Test
    public void editUserSuccessful() throws Exception {
        String newUserEmail = "test@test.com";
        String userLogin = "user";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("login", "user");

        ResultActions resultActions = mockMvc.perform(get(BASE_URL + "/users").params(params)
            .with(getRequestPostProcessorForUser(testAdminUser()))
            .with(csrf()));

        String html = resultActions.andReturn().getResponse().getContentAsString();
        Document doc = Jsoup.parse(html);
        Element usersTable = doc.getElementById("usersTable");
        Elements trElements = usersTable.getElementsByTag("tbody").first().getElementsByTag("tr");
        Element userElement = trElements.get(0);
        String userId = userElement.getElementsByTag("td").get(0).text();

        params = new LinkedMultiValueMap<>();
        params.add("id", userId);
        params.add("login", userLogin);
        params.add("name", "name");
        params.add("email", newUserEmail);
        resultActions = mockMvc.perform(patch(BASE_URL + "/users").params(params)
            .with(getRequestPostProcessorForUser(testAdminUser()))
            .with(csrf()));

        ModelAndView modelAndView = resultActions.andReturn().getModelAndView();

        Assertions.assertNotNull(modelAndView);
        boolean success = modelAndView.getModel().containsKey("success");
        Assertions.assertTrue(success);

        params = new LinkedMultiValueMap<>();
        params.add("login", "user");

        resultActions = mockMvc.perform(get(BASE_URL + "/users").params(params)
            .with(getRequestPostProcessorForUser(testAdminUser()))
            .with(csrf()));

        html = resultActions.andReturn().getResponse().getContentAsString();
        doc = Jsoup.parse(html);
        usersTable = doc.getElementById("usersTable");
        trElements = usersTable.getElementsByTag("tbody").first().getElementsByTag("tr");
        userElement = trElements.get(0);
        String userEmail = userElement.getElementsByTag("td").get(3).text();

        Assertions.assertEquals(newUserEmail, userEmail);
    }

    @Test
    public void editUserFailedWrongEmail() throws Exception {
        String newUserEmail = "test";
        String userLogin = "user";

        ResultActions resultActions = mockMvc
            .perform(get(BASE_URL + "/users").with(getRequestPostProcessorForUser(testAdminUser())).with(csrf()));

        String html = resultActions.andReturn().getResponse().getContentAsString();
        Document doc = Jsoup.parse(html);
        Element usersTable = doc.getElementById("usersTable");
        Elements trElements = usersTable.getElementsByTag("tbody").first().getElementsByTag("tr");
        Element userElement = trElements.get(0);
        String userId = userElement.getElementsByTag("td").get(0).text();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("id", userId);
        params.add("login", userLogin);
        params.add("name", "name");
        params.add("email", newUserEmail);
        resultActions = mockMvc.perform(patch(BASE_URL + "/users").params(params)
            .with(getRequestPostProcessorForUser(testAdminUser()))
            .with(csrf()));

        ModelAndView modelAndView = resultActions.andReturn().getModelAndView();

        Assertions.assertNotNull(modelAndView);
        boolean error = modelAndView.getModel().containsKey("error");
        Assertions.assertTrue(error);
    }

    @Test
    public void editUserFailedSuchLoginExists() throws Exception {
        String newUserLogin = "user_new";

        ResultActions resultActions = mockMvc
            .perform(get(BASE_URL + "/users").with(getRequestPostProcessorForUser(testAdminUser())).with(csrf()));

        String html = resultActions.andReturn().getResponse().getContentAsString();
        Document doc = Jsoup.parse(html);
        Element usersTable = doc.getElementById("usersTable");
        Elements trElements = usersTable.getElementsByTag("tbody").first().getElementsByTag("tr");
        Element userElement = trElements.get(1);
        String userId = userElement.getElementsByTag("td").get(0).text();
        String userEmail = userElement.getElementsByTag("td").get(3).text();
        String userName = userElement.getElementsByTag("td").get(2).text();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("id", userId);
        params.add("login", newUserLogin);
        params.add("name", userName);
        params.add("email", userEmail);
        resultActions = mockMvc.perform(patch(BASE_URL + "/users").params(params)
            .with(getRequestPostProcessorForUser(testAdminUser()))
            .with(csrf()));

        ModelAndView modelAndView = resultActions.andReturn().getModelAndView();

        Assertions.assertNotNull(modelAndView);
        boolean error = modelAndView.getModel().containsKey("error");
        Assertions.assertTrue(error);
    }

    @Test
    public void deleteUserSuccessful() throws Exception {
        ResultActions resultActions = mockMvc
            .perform(get(BASE_URL + "/users").with(getRequestPostProcessorForUser(testAdminUser())).with(csrf()));

        String html = resultActions.andReturn().getResponse().getContentAsString();
        Document doc = Jsoup.parse(html);
        Element usersTable = doc.getElementById("usersTable");
        Elements trElements = usersTable.getElementsByTag("tbody").last().getElementsByTag("tr");
        Assertions.assertTrue(trElements.size() > 0);

        Element userElement = trElements.get(0);
        String userId = userElement.getElementsByTag("td").get(0).text();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("id", userId);
        resultActions = mockMvc.perform(delete(BASE_URL + "/users").params(params)
            .with(getRequestPostProcessorForUser(testAdminUser()))
            .with(csrf()));

        ModelAndView modelAndView = resultActions.andReturn().getModelAndView();

        Assertions.assertNotNull(modelAndView);
        boolean error = modelAndView.getModel().containsKey("error");
        Assertions.assertFalse(error);
        userDatabaseInitService.initUserDatabase();
    }

    @Test
    public void deleteUserFailed() throws Exception {

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("id", "-1");
        ResultActions resultActions = mockMvc.perform(delete(BASE_URL + "/users").params(params)
            .with(getRequestPostProcessorForUser(testAdminUser()))
            .with(csrf()));

        ModelAndView modelAndView = resultActions.andReturn().getModelAndView();

        Assertions.assertNotNull(modelAndView);
        boolean error = modelAndView.getModel().containsKey("error");
        Assertions.assertTrue(error);
    }

}
