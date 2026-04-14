const minSelection = $minSelection$;

function selectedChanged() {
    const disabled = document.querySelectorAll(".movie-checkbox:checked").length < minSelection;

    document.querySelectorAll(".roulette-button").forEach(button => {
        button.disabled = disabled;
    });
}

selectedChanged();
