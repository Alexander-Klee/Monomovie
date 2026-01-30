let hasNextPage = true;
let lastCursor = "$endCursor$";
let isLoading = false;

function getMoreMovies(infinite_list, callback) {
    if (isLoading) return;
    isLoading = true;

    const currentParams = new URLSearchParams(window.location.search);
    let currentTitle = currentParams.get('title');

    let formData = new URLSearchParams();
    if (currentTitle) formData.append("title", currentTitle);
    if (lastCursor) formData.append("cursor", lastCursor);

    fetch("/moreSearchResults?" + formData.toString(), {
        method: "POST"
    }).then(response => {
        if (!response.ok) {
            throw new Error(`Server error: ${response.status}`);
        }
        return response.json();
    }).then(data => {
        const htmlMap = data.html || {};

        const allIds = Object.keys(htmlMap);
        const filteredIds = allIds.filter(id => !document.getElementById(`movie-item-${id}`));
        const idsToUse = filteredIds.length > 0 ? filteredIds : allIds;

        console.log(`infinite-scroll: adding ${idsToUse.length} of ${allIds.length} new items: ${idsToUse}`)

        const htmlToInsert = idsToUse.map(id => htmlMap[id]).join('');

        if (htmlToInsert) {
            infinite_list.insertAdjacentHTML('beforeend', htmlToInsert);
        }

        hasNextPage = data.hasNextPage;
        lastCursor = data.cursor;
    }).catch(error => {
        console.error("Fetch error:", error);
    }).finally(() => {
        isLoading = false;
        if (callback) {
            callback();
        }
    });
}

function handleScroll(infinite_list) {
    if (main.scrollTop + main.clientHeight >= main.scrollHeight && hasNextPage) {
        getMoreMovies(infinite_list, () => handleScroll(infinite_list));
    }
}

document.addEventListener("DOMContentLoaded", () => {
    const infinite_list = document.getElementById('infinite-list');

    main.addEventListener("scroll", () => handleScroll(infinite_list));
    handleScroll(infinite_list);
})
