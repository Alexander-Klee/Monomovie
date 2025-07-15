let hasNextPage = true;
let lastCursor = "$endCursor$";
let isLoading = false;

function getMoreMovies() {
    if (isLoading) return;
    isLoading = true;

    const currentParams = new URLSearchParams(window.location.search);
    let currentTitle = currentParams.get('title');

    let formData = new URLSearchParams();
    if (currentTitle) formData.append("title", currentTitle);
    if (lastCursor) formData.append("cursor", lastCursor);

    fetch("/moreSearchResults?" + formData.toString(), {
        method: "POST"
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Server error: ${response.status}`);
            }
            return response.json();
        }).then(data => {
        document.querySelector(".movie-list").insertAdjacentHTML('beforeend', data.html );
        hasNextPage = data.hasNextPage;
        lastCursor = data.cursor;
    })
        .catch(error => {
            console.error("Fetch error:", error);
        })
        .finally(() => {
            isLoading = false;
        });
}

window.addEventListener("scroll", () => {
    if (document.documentElement.scrollTop + document.documentElement.clientHeight >= document.documentElement.scrollHeight
        && hasNextPage) {
        getMoreMovies();
    }
});