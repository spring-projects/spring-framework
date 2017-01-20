function render(template, model) {
    return template.replace("{{title}}", model.title).replace("{{body}}", model.body);
}

function renderWithUrl(template, model, url) {
    return template.replace("{{title}}", "Check url parameter").replace("{{body}}", url);
}
