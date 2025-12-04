function watch(movieId, buttonElement) {
    const useElement = buttonElement.querySelector('use');
    const currentIcon = useElement.getAttribute('href');

    if (currentIcon === "#eye-icon") {
        deleteWatch(movieId, useElement);
    } else {
        setWatch(movieId, useElement);
    }
}

function deleteWatch(movieId, useElement) {
    fetch(`/watch/${movieId}`, {
        method: 'DELETE'
    })
    .then(() => {
        useElement.setAttribute('href', '#eye-plus-icon')
    })
    .catch(error => {
        console.error("Delete watch error:", error);
    });
}

function setWatch(movieId, useElement) {
    fetch(`/watch/${movieId}`, {
        method: 'POST'
    })
    .then(() => {
        useElement.setAttribute('href', '#eye-icon')
    })
    .catch(error => {
        console.error("Watch error:", error);
    });
}