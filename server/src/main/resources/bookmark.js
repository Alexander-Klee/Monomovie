window.setBookmark = function setBookmark(movieId, useElement, bookmarkIcon) {
    fetch(`/bookmark/${movieId}`, {
        method: 'POST'
    }).catch(error => {
        console.error("Bookmark error:", error);
    });
}

window.deleteBookmark = function deleteBookmark(movieId, useElement, bookmarkIcon) {
    fetch(`/bookmark/${movieId}`, {
        method: 'DELETE'
    }).catch(error => {
        console.error("Delete bookmark error:", error);
    });
}
