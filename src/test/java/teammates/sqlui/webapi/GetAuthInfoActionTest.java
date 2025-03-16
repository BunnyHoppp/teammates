package teammates.sqlui.webapi;

import java.util.ArrayList;
import java.util.Arrays;

import jakarta.servlet.http.Cookie;

import org.testng.annotations.Test;

import teammates.common.datatransfer.UserInfo;
import teammates.common.util.Const;
import teammates.common.util.StringHelper;
import teammates.ui.output.AuthInfo;
import teammates.ui.webapi.GetAuthInfoAction;
import teammates.ui.webapi.JsonResult;

/**
 * SUT: {@link GetAuthInfoAction}.
 */
public class GetAuthInfoActionTest extends BaseActionTest<GetAuthInfoAction> {

    @Override
    String getActionUri() {
        return Const.ResourceURIs.AUTH;
    }

    @Override
    String getRequestMethod() {
        return GET;
    }

    @Test
    void testExecute_noLoggedInUser() {
        logoutUser();

        GetAuthInfoAction a = getAction();
        JsonResult r = getJsonResult(a);

        AuthInfo output = (AuthInfo) r.getOutput();
        assertEquals(createLoginUrl("", Const.WebPageURIs.STUDENT_HOME_PAGE), output.getStudentLoginUrl());
        assertEquals(createLoginUrl("", Const.WebPageURIs.INSTRUCTOR_HOME_PAGE), output.getInstructorLoginUrl());
        assertEquals(createLoginUrl("", Const.WebPageURIs.ADMIN_HOME_PAGE), output.getAdminLoginUrl());
        assertEquals(createLoginUrl("", Const.WebPageURIs.MAINTAINER_HOME_PAGE), output.getMaintainerLoginUrl());
        assertNull(output.getUser());
        assertFalse(output.isMasquerade());
    }

    @Test
    void testExecute_noLoggedInUser_hasNextUrlParameter() {
        logoutUser();
        String nextUrl = "/web/join";

        String[] params = new String[] {
                "nextUrl", nextUrl,
        };

        GetAuthInfoAction a = getAction(params);
        JsonResult r = getJsonResult(a);

        AuthInfo output = (AuthInfo) r.getOutput();
        assertEquals(createLoginUrl("", nextUrl), output.getStudentLoginUrl());
        assertEquals(createLoginUrl("", nextUrl), output.getInstructorLoginUrl());
        assertEquals(createLoginUrl("", nextUrl), output.getAdminLoginUrl());
        assertEquals(createLoginUrl("", nextUrl), output.getMaintainerLoginUrl());
        assertNull(output.getUser());
        assertFalse(output.isMasquerade());
    }

    @Test
    void testExecute_loggedInAsInstructor() {
        loginAsInstructor("idOfInstructor1OfCourse1");

        GetAuthInfoAction a = getAction();
        JsonResult r = getJsonResult(a);

        AuthInfo output = (AuthInfo) r.getOutput();
        assertNull(output.getStudentLoginUrl());
        assertNull(output.getInstructorLoginUrl());
        assertNull(output.getAdminLoginUrl());
        assertNull(output.getMaintainerLoginUrl());
        assertFalse(output.isMasquerade());

        UserInfo user = output.getUser();
        assertFalse(user.isAdmin);
        assertTrue(user.isInstructor);
        assertFalse(user.isStudent);
        assertEquals("idOfInstructor1OfCourse1", user.id);
    }

    @Test
    void testExecute_loggedInAsUnregisteredUser() {
        loginAsUnregistered("unregisteredId");

        GetAuthInfoAction a = getAction();
        JsonResult r = getJsonResult(a);

        AuthInfo output = (AuthInfo) r.getOutput();
        assertNull(output.getStudentLoginUrl());
        assertNull(output.getInstructorLoginUrl());
        assertNull(output.getAdminLoginUrl());
        assertNull(output.getMaintainerLoginUrl());
        assertFalse(output.isMasquerade());

        UserInfo user = output.getUser();
        assertFalse(user.isAdmin);
        assertFalse(user.isInstructor);
        assertFalse(user.isStudent);
        assertEquals("unregisteredId", user.id);
    }

    @Test
    void testExecute_addCsrfTokenCookies_noLoggedInUser() {
        String expectedCsrfToken = StringHelper.encrypt("1234");
        String[] emptyParams = new String[] {};

        logoutUser();

        GetAuthInfoAction a = getAction(emptyParams);
        JsonResult r = getJsonResult(a);

        assertEquals(expectedCsrfToken, r.getCookies().get(0).getValue());
    }

    @Test
    void testExecute_addCsrfTokenCookies_fakeCsrfToken() {
        String expectedCsrfToken = StringHelper.encrypt("1234");
        String[] emptyParams = new String[] {};

        loginAsInstructor("idOfInstructor1OfCourse1");

        Cookie cookieToAdd = new Cookie(Const.SecurityConfig.CSRF_COOKIE_NAME, "someFakeCsrfToken");

        GetAuthInfoAction a = getActionWithCookie(new ArrayList<>(Arrays.asList(cookieToAdd)), emptyParams);
        JsonResult r = getJsonResult(a);

        assertEquals(expectedCsrfToken, r.getCookies().get(0).getValue());
    }

    @Test
    void testExecute_addCsrfTokenCookies_userLoggedInWithNonExistingCsrfToken() {
        String expectedCsrfToken = StringHelper.encrypt("1234");
        String[] emptyParams = new String[] {};

        loginAsInstructor("idOfInstructor1OfCourse1");

        GetAuthInfoAction a = getAction(emptyParams);
        JsonResult r = getJsonResult(a);

        assertEquals(expectedCsrfToken, r.getCookies().get(0).getValue());
    }

    @Test
    void testExecute_addCsrfTokenCookies_userLoggedInWithMatchedCsrfToken() {
        String[] emptyParams = new String[] {};

        loginAsInstructor("idOfInstructor1OfCourse1");

        Cookie cookieToAdd = new Cookie(Const.SecurityConfig.CSRF_COOKIE_NAME,
                StringHelper.encrypt("1234"));

        GetAuthInfoAction a = getActionWithCookie(new ArrayList<>(Arrays.asList(cookieToAdd)), emptyParams);
        JsonResult r = getJsonResult(a);

        assertEquals(0, r.getCookies().size());
    }

    @Test
    void testAccessControl_adminCanAccess() {
        loginAsAdmin();
        verifyCanAccess();
    }

    @Test
    void testAccessControl_unRegisteredUserCanAccess() {
        loginAsUnregistered("unregistered user");
        verifyCanAccess();
    }

    @Test
    void testAccessControl_noLoginCanAccess() {
        logoutUser();
        verifyCanAccess();
    }

    @Test
    void testAccessControl_nonAdminCannotMasquerade() {
        loginAsInstructor("idOfInstructor1OfCourse1");
        verifyCannotMasquerade("idOfAnotherInstructor");
    }

    String createLoginUrl(String frontendUrl, String nextUrl) {
        return Const.WebPageURIs.LOGIN + "?nextUrl=" + frontendUrl + nextUrl;
    }

}
