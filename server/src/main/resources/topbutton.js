function gotoTop() {
    main.scrollTo({
        top: 0,
        behavior: 'smooth'
    });
}

function scrollTopButton() {
    top_button.dataset.shown = main.scrollTop > 1000;
}

main.addEventListener('scroll', scrollTopButton);