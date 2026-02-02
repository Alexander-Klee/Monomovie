let hasNextPage = true;
let lastCursor = null;
let isLoading = false;

async function getMoreMovies(infiniteList, sentinel) {
    if (isLoading || !hasNextPage) return;
    isLoading = true;

    try {
        const currentParams = new URLSearchParams(window.location.search);
        const currentTitle = currentParams.get('title');

        const formData = new URLSearchParams();
        if (currentTitle) formData.append("title", currentTitle);
        if (lastCursor) formData.append("cursor", lastCursor);

        const response = await fetch("/search/results?" + formData.toString(), { method: "POST" });
        if (!response.ok) throw new Error(`Server error: ${response.status}`);
        const data = await response.json();

        const htmlMap = data.html || {};
        const allIds = Object.keys(htmlMap);
        const filteredIds = allIds.filter(id => !document.getElementById(`movie-item-${id}`));
        const idsToUse = filteredIds.length > 0 ? filteredIds : allIds;

        const htmlToInsert = idsToUse.map(id => htmlMap[id]).join('');
        if (htmlToInsert) {
            if (!lastCursor) {
                const header = document.createElement("h4");
                header.innerText = "Search Results:";
                infiniteList.insertAdjacentElement('beforebegin', header)
            }
            sentinel.insertAdjacentHTML('beforebegin', htmlToInsert);
        }

        hasNextPage = !!data.hasNextPage;

        if (!hasNextPage) {
            const notice = document.createElement("h3");
            notice.innerText = (lastCursor || htmlToInsert) ? "No more results found." : "No results found.";
            infiniteList.append(notice);
        }

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

    function isElementVisibleInRoot(el, root, extra = 400) {
        const rect = el.getBoundingClientRect();
        const rootRect = root.getBoundingClientRect();
        return rect.bottom >= (rootRect.top - extra) && rect.top <= (rootRect.bottom + extra);
    }

    async function handleEndReached() {
        console.log('requesting more movies');
        await getMoreMovies(infiniteList, sentinel);
        if (!hasNextPage) {
            console.log('infinite scroll is not so infinite after all');
            observer.disconnect();
            sentinel.remove();
        } else {
            // move sentinel to the end so newly added items are before it
            infiniteList.appendChild(sentinel);
            console.log('more movies added')
        }
    }

    const observer = new IntersectionObserver(async (entries) => {
        for (const entry of entries) {
            if (entry.isIntersecting && hasNextPage && !isLoading) {
                do {
                    await handleEndReached();
                } while (hasNextPage && !isLoading && isElementVisibleInRoot(sentinel, observerRoot));
            }
        }
    }, {
        root: observerRoot,
        rootMargin: '300px',
        threshold: 0.01
    });

    observer.observe(sentinel);
});
