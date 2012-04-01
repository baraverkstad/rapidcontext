/**
 * @namespace Provides functions for form validation.
 */
Validator = {};

/**
 * The non-empty text field validator function. This validator
 * trims the field value and checks if the field text is empty.
 */
Validator.NOT_EMPTY = function(widget) {
    var text = widget.getText();
    var str = MochiKit.Format.strip(text);

    if (text != str) {
        widget.setText(str);
    }
    return (str == "") ? "Blank Value" : null;
}

/**
 * The integer number text field validator function. This
 * validator trims the field value and checks if the field text
 * contains an integer number.
 */
Validator.INTEGER = function(widget) {
    var str;
    var num;

    str = Validator.NOT_EMPTY(widget);
    if (str != null) {
        return str;
    }
    str = widget.getText();
    try {
        num = parseInt(str);
    } catch (e) {
        num = null;
    }
    if (num == null || num.toFixed(0) != str) {
        return "Not Numeric";
    }
    return null;
}
