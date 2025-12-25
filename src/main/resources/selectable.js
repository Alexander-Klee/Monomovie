function selectedChanged() {
    const disabled = document.querySelectorAll(".movie-checkbox:checked").length <= 1;

    document.querySelectorAll(".roulette-button").forEach(button => {
        button.disabled = disabled;
    });
}

selectedChanged();
