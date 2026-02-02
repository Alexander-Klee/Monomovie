let hasNextPage = true;
let lastCursor = "$endCursor$";
let isLoading = false;

async function getMoreMovies(infiniteList) {
    if (isLoading || !hasNextPage) return;
    isLoading = true;

    try {
        const currentParams = new URLSearchParams(window.location.search);
        const currentTitle = currentParams.get('title');

        const formData = new URLSearchParams();
        if (currentTitle) formData.append("title", currentTitle);
        if (lastCursor) formData.append("cursor", lastCursor);

        const response = await fetch("/moreSearchResults?" + formData.toString(), { method: "POST" });
        if (!response.ok) throw new Error(`Server error: ${response.status}`);
        const data = await response.json();

        const htmlMap = data.html || {};
        const allIds = Object.keys(htmlMap);
        const filteredIds = allIds.filter(id => !document.getElementById(`movie-item-${id}`));
        const idsToUse = filteredIds.length > 0 ? filteredIds : allIds;

        const htmlToInsert = idsToUse.map(id => htmlMap[id]).join('');
        if (htmlToInsert) {
            // insert before sentinel if sentinel exists, otherwise at end
            const sentinel = document.getElementById('infinite-sentinel');
            if (sentinel) {
                sentinel.insertAdjacentHTML('beforebegin', htmlToInsert);
            } else {
                infiniteList.insertAdjacentHTML('beforeend', htmlToInsert);
            }
        }

        hasNextPage = !!data.hasNextPage;
        lastCursor = data.cursor;
    } catch (err) {
        console.error("Fetch error:", err);
    } finally {
        isLoading = false;
    }
}

document.addEventListener("DOMContentLoaded", async () => {
    const infiniteList = document.getElementById('infinite-list');
    if (!infiniteList) {
        console.error("No infinite-list found.");
        return;
    }

    const scrollContainerElement = document.getElementById('main');
    const observerRoot = scrollContainerElement || null;
    const sentinel = document.getElementById('infinite-sentinel');

    if (!sentinel) {
        console.error('infinite-sentinel was not found');
        return;
    }

    async function handleEndReached() {
        await getMoreMovies(infiniteList);
        infiniteList.appendChild(sentinel);
        if (!hasNextPage) observer.disconnect();
    }

    const observer = new IntersectionObserver(async (entries) => {
        for (const entry of entries) {
            if (entry.isIntersecting && hasNextPage && !isLoading) {
                await handleEndReached()
            }
        }
    }, {
        root: observerRoot,
        rootMargin: '300px',
        threshold: 0.01
    });

    observer.observe(sentinel);

    if (infiniteList.children.length === 0) {
        await handleEndReached();
    }
});
