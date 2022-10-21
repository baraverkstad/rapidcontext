/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2022 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE for more details.
 */

// Namespace initialization
if (typeof(RapidContext) == "undefined") {
    RapidContext = {};
}
RapidContext.Widget = RapidContext.Widget || {};

/**
 * Checks if the specified object is a form field. Any non-null object that
 * looks like a DOM node and is either an standard HTML form field (`<input>`,
 * `<textarea>` or `<select>`) or one with a "value" property will cause this
 * function to return `true`. Otherwise, `false` will be returned.
 *
 * @param {Object} obj the object to check
 *
 * @return {Boolean} `true` if the object looks like a form field, or
 *         `false` otherwise
 *
 * @static
 */
RapidContext.Widget.isFormField = function (obj) {
    if (!RapidContext.Util.isHTML(obj) || typeof(obj.tagName) !== "string") {
        return false;
    }
    var tagName = obj.tagName.toUpperCase();
    return tagName == "INPUT" ||
           tagName == "TEXTAREA" ||
           tagName == "SELECT" ||
           RapidContext.Widget.isWidget(obj, "Field");
};
