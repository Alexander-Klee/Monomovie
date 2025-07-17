function watch(movieId, el) {
    if (el.classList.contains('watched')) {
        deleteWatch(movieId, el);
    } else {
        setWatch(movieId, el);
    }
}

function deleteWatch(movieId, el) {
    fetch(`/watch/${movieId}`, {
        method: 'DELETE'
    })
        .then(() => {
            el.classList.remove('watched');
        })
        .catch(error => {
            console.error("Delete watch error:", error);
        });
}

function setWatch(movieId, el) {
    fetch(`/watch/${movieId}`, {
        method: 'POST'
    })
        .then(() => {
            el.classList.add('watched');
        })
        .catch(error => {
            console.error("Watch error:", error);
        });
}