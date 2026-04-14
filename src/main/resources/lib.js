document.parseHtml = function(html) {
    const template = document.createElement('template');
    template.innerHTML = html;
    if (template.content.childElementCount !== 1) {
        console.warn('Expected exactly one root element in HTML snippet, but found', template.content.childElementCount);
    }
    return template.content.firstChild;
}
