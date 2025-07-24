function gotoTop() {
    main.scrollTo({
        top: 0,
        behavior: 'smooth'
    });
}

function scrollTopButton() {
    if (main.scrollTop > 1000) {
        top_button.classList.add('show-top-button');
    } else {
        top_button.classList.remove('show-top-button');
    }
}

main.addEventListener('scroll', scrollTopButton);