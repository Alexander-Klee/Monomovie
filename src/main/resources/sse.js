const mode = '$mode$';

document.addEventListener("DOMContentLoaded", () => {
    const bookmarkList = document.getElementById('movie-list');

    const eventSource = new EventSource('/sse-stream?mode=' + mode);

    eventSource.onmessage = function(event) {
        console.log(event.data)
        if (event.data === "heartbeat") return;
        const data = JSON.parse(event.data);
        console.log('Received SSE:', data);
        const elements = document.querySelectorAll(`#${data.kind.toLowerCase()}-${data.id}`);
        const className = data.kind.toLowerCase() === 'bookmark' ? 'bookmarked' : 'watched';
        switch (data.type) {
            case 'de.amklee.monomovie.components.SseEvent.Add':
                if (elements.length > 0) {
                    elements.forEach(element => element.classList.add(className));
                } else if (bookmarkList && data.insert) {
                    const listItem = document.createElement('li');
                    bookmarkList.prepend(listItem);
                    listItem.outerHTML = data.body;
                }
                break;
            case 'de.amklee.monomovie.components.SseEvent.Remove':
                elements.forEach((element) => element.classList.remove(className));
                break;
            default:
                console.warn('Unknown event type:', data.type);
        }
    };

    eventSource.onerror = function(err) {
        console.error('EventSource failed:', err);
        eventSource.close();
    };
})
