function bookmark(movieId, el, isDoubleClick) {
    const isSmallScreen = window.matchMedia("(max-width: 600px)").matches;
    if (isSmallScreen !== isDoubleClick) return;

    // TODO the previous element from el is the checkbox, so we can use it to uncheck all checkboxes with the same movieId
    // el.parentNode
    document.querySelectorAll("#" + movieId).forEach(checkbox => {
        checkbox.checked = false;
    });

    if (el.classList.contains('bookmarked')) {
        deleteBookmark(movieId, el);
    } else {
        setBookmark(movieId, el);
    }

    return false; // prevent default action
}

function setBookmark(movieId, el) {
    fetch(`/bookmark/${movieId}`, {
        method: 'POST'
    })
        .then(() => {
            el.classList.add('bookmarked');
        })
        .catch(error => {
            console.error("Bookmark error:", error);
        });
}

function deleteBookmark(movieId, el) {
    fetch(`/bookmark/${movieId}`, {
        method: 'DELETE'
    })
        .then(() => {
            el.classList.remove('bookmarked');
        })
        .catch(error => {
            console.error("Delete bookmark error:", error);
        });
}