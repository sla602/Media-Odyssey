window.AuthModalFlow = (() => {
    const DEFAULT_AUTH_ERROR_MESSAGE = "Something went wrong on our end. Please try again later.";
    const DEFAULT_REQUIRED_FIELDS_MESSAGE = "Please fill out all required fields to continue.";
    const AUTH_INVALID_OR_NOT_FOUND = "Invalid email or password. Please try again.";

    const AUTH_STATUS_MESSAGES = Object.freeze({
        AUTH_LOGIN_SUCCESS: "Login successful.",
        AUTH_LOGIN_SUCCESS_UNVERIFIED: "Login successful. Don't forget to verify your email when you can.",
        AUTH_REGISTER_SUCCESS: "Account created! Please check your inbox for a verification link.",
        AUTH_RESEND_SUCCESS: "We've sent a new verification link. Please check your inbox.",
        AUTH_VERIFY_SUCCESS: "Your email is verified! You can now log in.",
        AUTH_PASSWORD_RESET_EMAIL_SENT: "If there's an account linked to that email, we've sent a reset link. Please check your inbox.",
        AUTH_PASSWORD_RESET_SUCCESS: "Your password has been reset. You can now log in.",
        AUTH_RATE_LIMITED: "Too many attempts. Please wait a moment and try again.",
        AUTH_INVALID_PASSWORD_RESET_TOKEN: "This reset link is invalid or has expired. Please request a new one.",
        AUTH_OAUTH_SIGN_IN_REQUIRED: "Looks like you signed up with a social account (like Google). Please log in using that service.",
        AUTH_INVALID_CREDENTIALS: AUTH_INVALID_OR_NOT_FOUND,
        AUTH_USER_NOT_FOUND: AUTH_INVALID_OR_NOT_FOUND,
        AUTH_DISABLED: "Your account has been disabled. Please contact support for help.",
        AUTH_LOCKED: "Your account has been locked. Please contact support for help.",
        AUTH_USER_ALREADY_VERIFIED: "This email is already registered. Please log in instead.",
        AUTH_INVALID_VERIFICATION_TOKEN: "This link is invalid or has expired. If you already verified your email, you can just log in. Otherwise, please request a new link.",
        AUTH_BAD_REQUEST: "Please check your information and try again.",
        AUTH_INTERNAL_ERROR: DEFAULT_AUTH_ERROR_MESSAGE,
    });

    function create({ statusModalId = "statusModal", statusBodyId = "statusModalBody", loadingModalId = "loadingModal" } = {}) {
        const statusModalEl = document.getElementById(statusModalId);
        const statusModalBody = document.getElementById(statusBodyId);
        const statusModalHeader = statusModalEl.querySelector(".modal-header");
        const statusModalFooter = statusModalEl.querySelector(".modal-footer");
        const statusModal = new bootstrap.Modal(statusModalEl);
        const loadingModalEl = document.getElementById(loadingModalId);
        const loadingModal = new bootstrap.Modal(loadingModalEl, {
            backdrop: "static",
            keyboard: false,
        });
        let loadingSpinnerEl = loadingModalEl.querySelector(".spinner-border");

        let redirectAfterClose = null;
        let redirectAfterShown = null;
        let pendingStatus = null;
        let loadingRequested = false;
        let loadingVisible = false;
        let hideAfterShown = false;

        function resetStatusModalPresentation() {
            statusModalBody.style.textAlign = "";
            statusModalBody.style.fontSize = "";
            statusModalBody.style.fontWeight = "";
            statusModalBody.style.lineHeight = "";

            if (statusModalHeader) {
                statusModalHeader.style.display = "";
            }

            if (statusModalFooter) {
                statusModalFooter.style.display = "";
            }
        }

        function showStatusModal(message, redirectUrl = null, options = {}) {
            const redirectOnShown = options.redirectOnShown === true;
            const useCheckmarkStyle = options.useCheckmarkStyle === true;

            resetStatusModalPresentation();
            statusModalBody.textContent = message;
            statusModalBody.style.textAlign = useCheckmarkStyle ? "center" : "";
            statusModalBody.style.fontSize = useCheckmarkStyle ? "3rem" : "";
            statusModalBody.style.fontWeight = useCheckmarkStyle ? "700" : "";
            statusModalBody.style.lineHeight = useCheckmarkStyle ? "1" : "";

            if (useCheckmarkStyle) {
                if (statusModalHeader) {
                    statusModalHeader.style.display = "none";
                }

                if (statusModalFooter) {
                    statusModalFooter.style.display = "none";
                }
            }

            redirectAfterClose = redirectOnShown ? null : redirectUrl;
            redirectAfterShown = redirectOnShown ? redirectUrl : null;
            statusModal.show();
        }

        function resetLoadingSpinner() {
            if (!loadingSpinnerEl) {
                return;
            }

            // Replace the node to reliably restart animation across browsers.
            const refreshedSpinner = loadingSpinnerEl.cloneNode(true);
            loadingSpinnerEl.replaceWith(refreshedSpinner);
            loadingSpinnerEl = refreshedSpinner;
        }

        function showLoadingModal() {
            pendingStatus = null;
            hideAfterShown = false;
            loadingRequested = true;
            resetLoadingSpinner();
            loadingModal.show();
        }

        function showStatusModalAfterLoading(message, redirectUrl = null, options = {}) {
            if (!loadingRequested) {
                showStatusModal(message, redirectUrl, options);
                return;
            }

            pendingStatus = { message, redirectUrl, options };

            if (loadingVisible) {
                loadingModal.hide();
                return;
            }

            hideAfterShown = true;
        }

        function resolveAuthStatusMessage(status, fallbackMessage, statusMessages = {}) {
            if (typeof status !== "string" || status.length === 0) {
                return fallbackMessage;
            }

            if (typeof status === "string" && Object.prototype.hasOwnProperty.call(statusMessages, status)) {
                return statusMessages[status];
            }

            if (typeof status === "string" && Object.prototype.hasOwnProperty.call(AUTH_STATUS_MESSAGES, status)) {
                return AUTH_STATUS_MESSAGES[status];
            }

            // Unrecognized statuses should default to a safe friendly message.
            return fallbackMessage;
        }

        function showAuthStatusAfterLoading(
            status,
            resBody,
            {
                fallbackMessage = DEFAULT_AUTH_ERROR_MESSAGE,
                redirectUrl = null,
                statusRedirects = {},
                statusMessages = {},
                redirectOnShownStatuses = [],
            } = {}
        ) {
            const apiMessage = typeof resBody?.message === "string" && resBody.message.length > 0
                ? resBody.message
                : fallbackMessage;
            const message = resolveAuthStatusMessage(status, apiMessage, statusMessages);
            const resolvedRedirect = typeof status === "string" && Object.prototype.hasOwnProperty.call(statusRedirects, status)
                ? statusRedirects[status]
                : redirectUrl;
            const shouldRedirectOnShown = typeof status === "string"
                && Array.isArray(redirectOnShownStatuses)
                && redirectOnShownStatuses.includes(status);

            showStatusModalAfterLoading(message, resolvedRedirect, {
                redirectOnShown: shouldRedirectOnShown,
            });
        }

        function setFormSubmitting(formEl, isSubmitting) {
            if (!formEl) {
                return;
            }

            const submitControls = formEl.querySelectorAll('button[type="submit"], input[type="submit"]');
            submitControls.forEach((control) => {
                control.disabled = isSubmitting;
                control.setAttribute("aria-disabled", String(isSubmitting));
            });

            formEl.setAttribute("aria-busy", String(isSubmitting));
        }

        function validateRequiredFields(values, message = DEFAULT_REQUIRED_FIELDS_MESSAGE) {
            const hasEmptyValue = values.some((value) => typeof value !== "string" || value.trim().length === 0);
            if (hasEmptyValue) {
                showStatusModal(message);
                return false;
            }

            return true;
        }

        statusModalEl.addEventListener("hidden.bs.modal", () => {
            if (redirectAfterClose) {
                const target = redirectAfterClose;
                redirectAfterClose = null;
                window.location.href = target;
            }
        });

        statusModalEl.addEventListener("shown.bs.modal", () => {
            if (redirectAfterShown) {
                const target = redirectAfterShown;
                redirectAfterShown = null;
                window.location.replace(target);
            }
        });

        loadingModalEl.addEventListener("shown.bs.modal", () => {
            loadingVisible = true;

            if (hideAfterShown) {
                loadingModal.hide();
            }
        });

        loadingModalEl.addEventListener("hidden.bs.modal", () => {
            loadingRequested = false;
            loadingVisible = false;
            hideAfterShown = false;

            if (!pendingStatus) {
                return;
            }

            const { message, redirectUrl, options } = pendingStatus;
            pendingStatus = null;
            showStatusModal(message, redirectUrl, options);
        });

        return {
            showLoadingModal,
            showStatusModal,
            showStatusModalAfterLoading,
            showAuthStatusAfterLoading,
            setFormSubmitting,
            validateRequiredFields,
        };
    }

    return { create };
})();
