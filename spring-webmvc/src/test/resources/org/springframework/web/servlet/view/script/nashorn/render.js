function render(template, model) {
    return template.replace("{{title}}", model.title).replace("{{body}}", model.body);
}

function renderWithUrl(template, model, url) {
    return template.replace("{{title}}", "Check url parameter").replace("{{body}}", url);
}

function renderWithMessages(template, model, url, messages) {
    return template.replace("{{title}}", messages.title).replace("{{body}}", messages.body);
}