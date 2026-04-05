window.AuthModalFlow = (() => {
    function create({ statusModalId = "statusModal", statusBodyId = "statusModalBody", loadingModalId = "loadingModal" } = {}) {
        const statusModalEl = document.getElementById(statusModalId);
        const statusModalBody = document.getElementById(statusBodyId);
        const statusModal = new bootstrap.Modal(statusModalEl);
        const loadingModalEl = document.getElementById(loadingModalId);
        const loadingModal = new bootstrap.Modal(loadingModalEl, {
            backdrop: "static",
            keyboard: false,
        });
        let loadingSpinnerEl = loadingModalEl.querySelector(".spinner-border");

        let redirectAfterClose = null;
        let pendingStatus = null;
        let loadingRequested = false;
        let loadingVisible = false;
        let hideAfterShown = false;

        function showStatusModal(message, redirectUrl = null) {
            statusModalBody.textContent = message;
            redirectAfterClose = redirectUrl;
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

        function showStatusModalAfterLoading(message, redirectUrl = null) {
            if (!loadingRequested) {
                showStatusModal(message, redirectUrl);
                return;
            }

            pendingStatus = { message, redirectUrl };

            if (loadingVisible) {
                loadingModal.hide();
                return;
            }

            hideAfterShown = true;
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

            const { message, redirectUrl } = pendingStatus;
            pendingStatus = null;
            showStatusModal(message, redirectUrl);
        });

        return {
            showLoadingModal,
            showStatusModal,
            showStatusModalAfterLoading,
            setFormSubmitting,
        };
    }

    return { create };
})();