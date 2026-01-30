function handleImageError(img) {
    console.log("image error:", img);
    const posterContainer = img.closest('.movie-poster');
    if (posterContainer) {
        posterContainer.classList.add('error');
    }
}
