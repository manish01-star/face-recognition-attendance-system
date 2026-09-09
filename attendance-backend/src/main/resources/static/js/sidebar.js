document.addEventListener("DOMContentLoaded", function () {

    const container =
        document.getElementById("sidebar-container");

    if (!container) {
        return;
    }

    fetch("/components/sidebar.html")
        .then(response => {

            if (!response.ok) {
                throw new Error(
                    "Unable to load sidebar"
                );
            }

            return response.text();

        })
        .then(html => {

            container.innerHTML = html;

            initializeSidebar();

        })
        .catch(error => {

            console.error(
                "Sidebar loading error:",
                error
            );

        });

});


function initializeSidebar() {

    const sidebar =
        document.getElementById("sidebar");

    const overlay =
        document.getElementById("sidebarOverlay");

    const mobileMenuBtn =
        document.getElementById("mobileMenuBtn");


    setActiveSidebar();

    setAdminName();


    if (mobileMenuBtn) {

        mobileMenuBtn.addEventListener(
            "click",
            function () {

                if (sidebar) {
                    sidebar.classList.add("show");
                }

                if (overlay) {
                    overlay.classList.add("show");
                }

            }
        );

    }


    if (overlay) {

        overlay.addEventListener(
            "click",
            closeSidebar
        );

    }


    document
        .querySelectorAll(".sidebar-link")
        .forEach(link => {

            link.addEventListener(
                "click",
                closeSidebar
            );

        });


    const logoutBtn =
        document.getElementById("logoutBtn");

    if (logoutBtn) {

        logoutBtn.addEventListener(
            "click",
            function () {

                localStorage.removeItem(
                    "accessToken"
                );

                localStorage.removeItem(
                    "adminUsername"
                );

                window.location.replace(
                    "/login.html"
                );

            }
        );

    }

}


function closeSidebar() {

    const sidebar =
        document.getElementById("sidebar");

    const overlay =
        document.getElementById("sidebarOverlay");


    if (sidebar) {
        sidebar.classList.remove("show");
    }

    if (overlay) {
        overlay.classList.remove("show");
    }

}


function setActiveSidebar() {

    let currentPage =
        window.location.pathname
            .split("/")
            .pop()
            .toLowerCase();


    if (!currentPage) {
        currentPage = "dashboard.html";
    }


    document
        .querySelectorAll(".sidebar-link")
        .forEach(link => {

            const page =
                (
                    link.dataset.page || ""
                ).toLowerCase();

            link.classList.remove("active");


            if (page === currentPage) {
                link.classList.add("active");
            }

        });

}


function setAdminName() {

    const username =
        localStorage.getItem(
            "adminUsername"
        );

    const adminName =
        document.getElementById(
            "adminName"
        );


    if (
        adminName &&
        username
    ) {

        adminName.innerText =
            username;

    }

}