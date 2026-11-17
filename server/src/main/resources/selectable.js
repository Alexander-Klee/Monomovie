const minSelection = $minSelection$;

window.selectedChanged = function selectedChanged() {
    const disabled = document.querySelectorAll(".movie-checkbox:checked").length < minSelection;

    document.querySelectorAll(".require-min-selection").forEach(button => {
        button.disabled = disabled;
    });
}

window.selectedChanged();
