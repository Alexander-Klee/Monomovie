function handleImageError(img) {
    console.log("image error:", img);
    const posterContainer = img.closest('.movie-poster');
    if (posterContainer) {
        posterContainer.dataset.error = true;
    }
}
