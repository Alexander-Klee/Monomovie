function deleteWatch(movieId, useElement) {
    fetch(`/watch/${movieId}`, {
        method: 'DELETE'
    }).catch(error => {
        console.error("Delete watch error:", error);
    });
}

function setWatch(movieId, useElement) {
    fetch(`/watch/${movieId}`, {
        method: 'POST'
    }).catch(error => {
        console.error("Watch error:", error);
    });
}
