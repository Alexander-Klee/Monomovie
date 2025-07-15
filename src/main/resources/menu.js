function showMenu() {
    const nav = document.querySelector('nav');
    nav.classList.toggle('nav-visible');
    document.body.classList.toggle('menu-open', nav.classList.contains('nav-visible'));
}
function closeMenu() {
    const nav = document.querySelector('nav');
    if (nav.classList.contains('nav-visible')) {
        nav.classList.remove('nav-visible');
        document.body.classList.remove('menu-open');
    }
}
