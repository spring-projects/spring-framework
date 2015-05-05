function render(template, model) {
    return template.replace("{{title}}", model.title).replace("{{body}}", model.body);
}