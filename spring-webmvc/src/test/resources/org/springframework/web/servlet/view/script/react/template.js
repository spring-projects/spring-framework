React.createClass({
    render: function() {
        return React.createElement("html", null, React.createElement("head", null, React.createElement("title", null, this.props.title)), React.createElement("body", null, React.createElement("p", null, this.props.body)));
    }
});