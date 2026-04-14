function bookmark(movieId, buttonElement) {
    const useElement = buttonElement.querySelector('use');
    const currentIcon = useElement.getAttribute('href');
    const movieItem = buttonElement.closest('.movie-item');
    const bookmarkIcon = movieItem.querySelector('.bookmark-icon');

    if (currentIcon === "#bookmark-icon") {
        deleteBookmark(movieId, useElement, bookmarkIcon);
    } else {
        setBookmark(movieId, useElement, bookmarkIcon);
    }
}

function setBookmark(movieId, useElement, bookmarkIcon) {
    fetch(`/bookmark/${movieId}`, {
        method: 'POST'
    }).then(r => {
        useElement.setAttribute('href', '#bookmark-icon');
        bookmarkIcon.classList.add('bookmarked');
    })
    .catch(error => {
        console.error("Bookmark error:", error);
    });
}

function deleteBookmark(movieId, useElement, bookmarkIcon) {
    fetch(`/bookmark/${movieId}`, {
        method: 'DELETE'
    }).then(r => {
        useElement.setAttribute('href', '#bookmark-plus-icon')
        bookmarkIcon.classList.remove('bookmarked');
    })
    .catch(error => {
        console.error("Delete bookmark error:", error);
    });
}
