document.addEventListener('DOMContentLoaded', () => {
    const container = document.getElementById('movie-list');

    container.addEventListener('click', (e) => {
        const btn = e.target.closest('.roulette-weight-button');
        if (!btn || !container.contains(btn)) return;

        const spinner = btn.closest('.roulette-weight-container');
        const input = spinner.querySelector('input[type="number"]');
        if (!input) return;

        if (btn.classList.contains('increment')) {
            input.stepUp();
        } else if (btn.classList.contains('decrement')) {
            input.stepDown();
        }
    });
});