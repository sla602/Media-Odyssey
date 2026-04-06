window.AuthModalFlow = (() => {
    const DEFAULT_AUTH_ERROR_MESSAGE = "An unexpected error occurred. Please try again later.";

    const AUTH_STATUS_MESSAGES = Object.freeze({
        AUTH_LOGIN_SUCCESS: "Login successful.",
        AUTH_LOGIN_SUCCESS_UNVERIFIED: "Login successful. Your email is not verified yet. Please verify your email when possible.",
        AUTH_REGISTER_SUCCESS: "Account created successfully. Please check your email to verify your account.",
        AUTH_RESEND_SUCCESS: "Successfully re-sent email verification link. Please check your email to verify your account.",
        AUTH_VERIFY_SUCCESS: "Your email has been verified successfully. You can now log in using the button below.",
        AUTH_PASSWORD_RESET_EMAIL_SENT: "If an eligible account exists, a password reset link has been sent. Please check your email.",
        AUTH_PASSWORD_RESET_SUCCESS: "Your password has been reset successfully. Please log in with your new password.",
        AUTH_INVALID_PASSWORD_RESET_TOKEN: "This password reset link is invalid or expired. Please request a new reset link.",
        AUTH_OAUTH_SIGN_IN_REQUIRED: "This account was created with an OAuth provider. Please continue via OAuth provider.",
        AUTH_INVALID_CREDENTIALS: "The email and/or password provided is incorrect. Please try again.",
        AUTH_USER_NOT_FOUND: "The email and/or password provided is incorrect. Please try again.",
        AUTH_DISABLED: "Your account is disabled. Please contact support.",
        AUTH_LOCKED: "Your account is locked. Please contact support.",
        AUTH_USER_ALREADY_VERIFIED: "The email is already registered. Please log in.",
        AUTH_INVALID_VERIFICATION_TOKEN: "This verification link is invalid or expired. If you have already previously verified your email, please continue to log in. Otherwise, please request a new one.",
        AUTH_BAD_REQUEST: "There was a problem with the input submitted. Please try again.",
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
            } = {}
        ) {
            const apiMessage = typeof resBody?.message === "string" && resBody.message.length > 0
                ? resBody.message
                : fallbackMessage;
            const message = resolveAuthStatusMessage(status, apiMessage, statusMessages);
            const resolvedRedirect = typeof status === "string" && Object.prototype.hasOwnProperty.call(statusRedirects, status)
                ? statusRedirects[status]
                : redirectUrl;

            showStatusModalAfterLoading(message, resolvedRedirect);
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
        };
    }

    return { create };
})();