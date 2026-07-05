window.AuthPageFlows = (() => {
    const AUTH_API = AuthRoutes.Api;
    const AUTH_PAGE = AuthRoutes.Page;
    const AUTH_QUERY = AuthRoutes.QueryParams;

    function createUi() {
        return AuthModalFlow.create();
    }

    function getFieldValue(formEl, selector, { trim = false } = {}) {
        const element = formEl.querySelector(selector);
        if (!(element instanceof HTMLInputElement) && !(element instanceof HTMLTextAreaElement)) {
            return "";
        }

        const value = element.value ?? "";
        return trim ? value.trim() : value;
    }

    function getCheckedValue(formEl, selector) {
        const element = formEl.querySelector(selector);
        return element instanceof HTMLInputElement ? element.checked : false;
    }

    function createRequest(url, init) {
        return { url, init };
    }

    function createJsonRequest(url, body, { method = "POST", headers = {}, credentials = "include" } = {}) {
        return createRequest(url, {
            method,
            headers: {
                "Content-Type": "application/json",
                ...headers,
            },
            credentials,
            body: JSON.stringify(body),
        });
    }

    function bindPageForm(formId, resolver, ui = null) {
        const formEl = document.getElementById(formId);
        if (!formEl) {
            return null;
        }

        const resolvedUi = ui ?? createUi();
        bindJsonForm(formEl, resolvedUi, resolver);
        return resolvedUi;
    }

    function readFormValues(formEl, fieldSpecs) {
        return fieldSpecs.reduce((values, fieldSpec) => {
            if (fieldSpec.checked) {
                values[fieldSpec.name] = getCheckedValue(formEl, fieldSpec.selector);
                return values;
            }

            values[fieldSpec.name] = getFieldValue(formEl, fieldSpec.selector, { trim: fieldSpec.trim === true });
            return values;
        }, {});
    }

    function buildSubmissionFromFields(ctx, {
        fields = [],
        requiredFields = null,
        requiredMessage,
        beforeBuild = null,
        buildRequest,
        statusRedirects = {},
        successStatus,
        onSuccess,
        fallbackMessage,
        statusMessages,
        redirectImmediatelyStatuses = [],
        redirectOnShownStatuses = [],
    }) {
        const values = readFormValues(ctx.formEl, fields);
        const requiredValues = (requiredFields ?? fields.filter((field) => field.required !== false).map((field) => field.name))
            .map((fieldName) => values[fieldName]);

        if (!ctx.ui.validateRequiredFields(requiredValues, requiredMessage)) {
            return null;
        }

        if (typeof beforeBuild === "function" && beforeBuild(values, ctx) === false) {
            return null;
        }

        return {
            request: buildRequest(values, ctx),
            statusRedirects,
            successStatus,
            onSuccess,
            fallbackMessage,
            statusMessages,
            redirectImmediatelyStatuses,
            redirectOnShownStatuses,
        };
    }

    function ensureMatchingPasswords(ui, firstPassword, secondPassword) {
        if (firstPassword !== secondPassword) {
            ui.showStatusModal("Passwords do not match.");
            return false;
        }

        return true;
    }

    function showMissingTokenState(ui, formEl, controlEl, message, redirectUrl) {
        if (controlEl) {
            controlEl.disabled = true;
        }

        formEl.setAttribute("aria-busy", "false");
        ui.showStatusModal(message, redirectUrl);
    }

    function bindJsonForm(formEl, ui, resolveSubmission) {
        if (!formEl) {
            return;
        }

        let isSubmitting = false;

        formEl.addEventListener("submit", async (event) => {
            event.preventDefault();

            if (isSubmitting) {
                return;
            }

            const submission = resolveSubmission({
                formEl,
                ui,
                getFieldValue,
                getCheckedValue,
                createRequest,
                createJsonRequest,
                ensureMatchingPasswords,
                showMissingTokenState,
            });

            if (!submission) {
                return;
            }

            isSubmitting = true;
            ui.setFormSubmitting(formEl, true);
            ui.showLoadingModal();

            try {
                const response = await fetch(submission.request.url, submission.request.init);
                const responseBody = await response.json();
                const status = responseBody?.status;

                const shouldRedirectImmediately = typeof status === "string"
                    && Array.isArray(submission.redirectImmediatelyStatuses)
                    && submission.redirectImmediatelyStatuses.includes(status);

                if (shouldRedirectImmediately) {
                    // The home page shows the verification reminder from account state.
                    const immediateRedirect = typeof status === "string"
                        && submission.statusRedirects
                        && Object.prototype.hasOwnProperty.call(submission.statusRedirects, status)
                        ? submission.statusRedirects[status]
                        : null;

                    if (typeof immediateRedirect === "string" && immediateRedirect.length > 0) {
                        window.location.replace(immediateRedirect);
                        return;
                    }
                }

                if (submission.successStatus && status === submission.successStatus
                        && typeof submission.onSuccess === "function") {
                    submission.onSuccess({ response, responseBody, ui });
                    return;
                }

                ui.showAuthStatusAfterLoading(status, responseBody, {
                    statusRedirects: submission.statusRedirects,
                    fallbackMessage: submission.fallbackMessage,
                    statusMessages: submission.statusMessages,
                });
            } catch (error) {
                ui.showAuthStatusAfterLoading(undefined, undefined);
            } finally {
                isSubmitting = false;
                ui.setFormSubmitting(formEl, false);
            }
        });
    }

    function initLoginPage() {
        const ui = createUi();
        bindPageForm("loginForm", (ctx) => buildSubmissionFromFields(ctx, {
            fields: [
                { name: "email", selector: "#email", trim: true },
                { name: "password", selector: "#password" },
                { name: "rememberMe", selector: "#rememberMe", checked: true, required: false },
            ],
            requiredFields: ["email", "password"],
            buildRequest: ({ email, password, rememberMe }) => createJsonRequest(AUTH_API.LOGIN, {
                email,
                password,
                rememberMe,
            }),
            successStatus: "AUTH_LOGIN_SUCCESS",
            onSuccess: () => {
                // Verified accounts do not need a success modal; the redirect is the feedback.
                window.location.replace("/");
            },
            statusRedirects: {
                AUTH_LOGIN_SUCCESS_UNVERIFIED: "/",
            },
            redirectImmediatelyStatuses: ["AUTH_LOGIN_SUCCESS_UNVERIFIED"],
        }), ui);

        const searchParams = new URLSearchParams(window.location.search);

        if (searchParams.get(AUTH_QUERY.SESSION_EXPIRED) === "true") {
            ui.showStatusModal("Your session has expired. Please log in again.");
        } else if (searchParams.get(AUTH_QUERY.OAUTH_ERROR) === "true") {
            ui.showStatusModal("Provider sign-in failed. If you previously registered with email/password, please log in with email/password instead.");
        }
    }

    function initSignupPage() {
        bindPageForm("signupForm", (ctx) => buildSubmissionFromFields(ctx, {
            fields: [
                { name: "email", selector: "#email", trim: true },
                { name: "password", selector: "#password" },
                { name: "confirmPassword", selector: "#confirmPassword" },
            ],
            requiredFields: ["email", "password", "confirmPassword"],
            beforeBuild: ({ password, confirmPassword }, { ui }) => ensureMatchingPasswords(ui, password, confirmPassword),
            buildRequest: ({ email, password }) => createJsonRequest(AUTH_API.REGISTER, {
                email,
                password,
            }),
            statusRedirects: {
                AUTH_REGISTER_SUCCESS: AUTH_PAGE.LOGIN,
            },
        }));
    }

    function initForgotPage() {
        bindPageForm("forgotPasswordForm", (ctx) => buildSubmissionFromFields(ctx, {
            fields: [
                { name: "email", selector: "#email", trim: true },
            ],
            requiredFields: ["email"],
            buildRequest: ({ email }) => createJsonRequest(AUTH_API.PASSWORD_FORGOT, {
                email,
            }),
            statusRedirects: {
                AUTH_PASSWORD_RESET_EMAIL_SENT: AUTH_PAGE.LOGIN,
                AUTH_PASSWORD_RESET_OAUTH_NOT_ALLOWED: AUTH_PAGE.LOGIN,
            },
        }));
    }

    function initResendPage() {
        bindPageForm("resendForm", (ctx) => buildSubmissionFromFields(ctx, {
            fields: [
                { name: "email", selector: "#email", trim: true },
            ],
            requiredFields: ["email"],
            buildRequest: ({ email }) => createJsonRequest(AUTH_API.RESEND, {
                email,
            }),
            statusRedirects: {
                AUTH_RESEND_SUCCESS: AUTH_PAGE.LOGIN,
            },
        }));
    }

    function initVerifyPage() {
        const verifyForm = document.getElementById("verifyForm");
        const verifyBtn = document.getElementById("verifyBtn");
        if (!verifyForm || !verifyBtn) {
            return;
        }

        const ui = createUi();
        const token = new URLSearchParams(window.location.search).get("token");

        if (!token) {
            showMissingTokenState(
                ui,
                verifyForm,
                verifyBtn,
                "This verification link is missing a token. Please request a new verification email.",
                AUTH_PAGE.RESEND
            );
            return;
        }

        bindJsonForm(verifyForm, ui, () => ({
            request: createRequest(`${AUTH_API.VERIFY}?token=${encodeURIComponent(token)}`, {
                method: "POST",
                headers: {
                    Accept: "application/json",
                },
                credentials: "include",
            }),
            statusRedirects: {
                AUTH_VERIFY_SUCCESS: AUTH_PAGE.LOGIN,
                AUTH_INVALID_VERIFICATION_TOKEN: AUTH_PAGE.RESEND,
            },
        }));
    }

    function initResetPage() {
        const resetPasswordForm = document.getElementById("resetPasswordForm");
        const resetButton = document.getElementById("resetButton");
        if (!resetPasswordForm || !resetButton) {
            return;
        }

        const ui = createUi();
        const token = new URLSearchParams(window.location.search).get("token");

        if (!token) {
            showMissingTokenState(
                ui,
                resetPasswordForm,
                resetButton,
                "This reset link is missing a token. Please request a new reset link.",
                AUTH_PAGE.FORGOT
            );
            return;
        }

        bindJsonForm(resetPasswordForm, ui, (ctx) => buildSubmissionFromFields(ctx, {
            fields: [
                { name: "newPassword", selector: "#newPassword" },
                { name: "confirmPassword", selector: "#confirmPassword" },
            ],
            requiredFields: ["newPassword", "confirmPassword"],
            beforeBuild: ({ newPassword, confirmPassword }, { ui: pageUi }) => ensureMatchingPasswords(pageUi, newPassword, confirmPassword),
            buildRequest: ({ newPassword }) => createJsonRequest(AUTH_API.PASSWORD_RESET, {
                token,
                newPassword,
            }),
            statusRedirects: {
                AUTH_PASSWORD_RESET_SUCCESS: AUTH_PAGE.LOGIN,
                AUTH_INVALID_PASSWORD_RESET_TOKEN: AUTH_PAGE.FORGOT,
                AUTH_PASSWORD_RESET_OAUTH_NOT_ALLOWED: AUTH_PAGE.LOGIN,
            },
        }));
    }

    function initialize() {
        initLoginPage();
        initSignupPage();
        initForgotPage();
        initResendPage();
        initVerifyPage();
        initResetPage();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initialize);
    } else {
        initialize();
    }

    return {
        initialize,
    };
})();
