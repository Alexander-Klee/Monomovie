const isSelection = $isSelection$;
const shareId = "$shareId$";

function injectUpdateCount(parent) {
    if (isSelection) return; // No need to update counts in selection mode
    parent.querySelectorAll('.roulette-weight-input').forEach(element => {
        element.onchange = () => {
            const count = parseInt(element.value) || 0;
            fetch('/roulette/shared/' + shareId + '/' + element.id, {
                method: 'POST',
                body: `${count}`
            }).catch(err => console.error('Failed to update count:', err));
        };
    });
}

document.addEventListener("DOMContentLoaded", () => {
    const bookmarkList = document.getElementById('movie-list');

    injectUpdateCount(document);

    const eventSource = new EventSource('/roulette/shared/' + shareId + '/sse?isSelection=' + isSelection);

    eventSource.onmessage = function(event) {
        if (event.data === "heartbeat") return;
        const data = JSON.parse(event.data);
        console.log('Received SSE:', data);
        const elements = document.querySelectorAll(`#${data.id}`);
        switch (data.type) {
            case 'de.amklee.monomovie.pages.RouletteSseEvent.Add':
                if (elements.length > 0) {
                    if (isSelection) {
                        elements.forEach(element => {
                            if (isSelection) {
                                element.selected = true;
                            }
                        });
                    }
                } else if (bookmarkList) {
                    const element = document.parseHtmlElement(data.body);
                    bookmarkList.prepend(element);
                    injectUpdateCount(element);
                }
                break;
            case 'de.amklee.monomovie.pages.RouletteSseEvent.Update':
                if (!isSelection && elements.length > 0) {
                    elements.forEach(element => {
                        element.value = data.count;
                    });
                }
                break
            case 'de.amklee.monomovie.pages.RouletteSseEvent.Remove':
                elements.forEach((element) => {
                    if (isSelection) {
                        element.selected = false;
                    } else {
                        document.querySelectorAll("#roulette-" + data.id).forEach(el => el.remove());
                    }
                });
                break;
            default:
                console.warn('Unknown event type:', data.type);
        }
    };

    eventSource.onerror = function(err) {
        //TODO reconnect on network change (or similar)
        console.error('EventSource failed:', err);
        eventSource.close();
    };
});
