const shareId = "$shareId$";

function injectUpdateCount(parent) {
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

    const eventSource = new EventSource('/roulette/shared/' + shareId + '/sse');

    eventSource.onmessage = function(event) {
        if (event.data === "heartbeat") return;
        const data = JSON.parse(event.data);
        console.log('Received SSE:', data);
        const elements = document.querySelectorAll(`#${data.id}`);
        switch (data.type) {
            case 'de.amklee.monomovie.pages.RouletteSseEvent.Add':
                if (elements.length <= 0 && bookmarkList) {
                    const element = document.parseHtmlElement(data.body);
                    bookmarkList.append(element);
                    injectUpdateCount(element);
                }
                break;
            case 'de.amklee.monomovie.pages.RouletteSseEvent.Update':
                elements.forEach(element => {
                    element.value = data.count;
                });
                break
            case 'de.amklee.monomovie.pages.RouletteSseEvent.Remove':
                document.querySelectorAll("#roulette-" + data.id).forEach(el => el.remove());
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
