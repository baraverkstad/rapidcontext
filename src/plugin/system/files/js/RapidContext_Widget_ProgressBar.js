/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2024 Per Cederberg. All rights reserved.
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
RapidContext.Widget = RapidContext.Widget || { Classes: {} };

/**
 * Creates a new progress bar widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {number} [attrs.min] the minimum range value, defaults to 0
 * @param {number} [attrs.max] the maximum range value, defaults to 100
 * @param {number} [attrs.value] the progress value, a number between `min`
 *            and `max`, defaults to 0
 * @param {number} [attrs.ratio] the progress ratio, a floating-point number
 *            between 0.0 and 1.0, defaults to 0.0
 * @param {number} [attrs.text] the additional information text, defaults to
 *            blank
 * @param {boolean} [attrs.noratio] the hide ratio (in percent) display flag,
 *            defaults to `false`
 * @param {boolean} [attrs.novalue] the hide value display flag, defaults to
 *            `false`
 * @param {boolean} [attrs.notime] the hide remaining time display flag,
 *            defaults to `false`
 * @param {boolean} [attrs.hidden] the hidden widget flag, defaults to false
 *
 * @return {Widget} the widget DOM node
 *
 * @class The progress bar widget class. Used to provide a dynamic progress
 *     meter, using a `<div>` HTML elements. The progress bar also provides a
 *     completion time estimation that is displayed in the bar. Whenever the
 *     range is modified, the time estimation is reset.
 * @extends RapidContext.Widget
 *
 * @example <caption>JavaScript</caption>
 * let attrs = { text: "Working", noratio: true, notime: true };
 * let w = RapidContext.Widget.ProgressBar(attrs);
 *
 * @example <caption>User Interface XML</caption>
 * <ProgressBar text="Working" noratio="true" notime="true" />
 */
RapidContext.Widget.ProgressBar = function (attrs) {
    let text = MochiKit.DOM.DIV({ "class": "widgetProgressBarText" });
    let meter = document.createElement("progress");
    let o = MochiKit.DOM.DIV({}, text, meter);
    RapidContext.Widget._widgetMixin(o, RapidContext.Widget.ProgressBar);
    o.addClass("widgetProgressBar");
    o.setAttrs(Object.assign({ min: 0, max: 100, value: 0 }, attrs));
    return o;
};

// Register widget class
RapidContext.Widget.Classes.ProgressBar = RapidContext.Widget.ProgressBar;

/**
 * Returns the widget container DOM node.
 *
 * @return {Node} returns null, since child nodes are not supported
 */
RapidContext.Widget.ProgressBar.prototype._containerNode = function () {
    return null;
};

/**
 * Updates the widget or HTML DOM node attributes. Note that updating the
 * value will automatically also update the ratio. All calls to this method
 * may trigger a new remaining time estimation.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {number} [attrs.min] the minimum range value, if modified the current
 *            value and ratio are reset
 * @param {number} [attrs.max] the maximum range value, if modified the current
 *            value and ratio are reset
 * @param {number} [attrs.value] the progress value, a number between `min`
 *            and `max`
 * @param {number} [attrs.ratio] the progress ratio, a floating-point number
 *            between 0.0 and 1.0
 * @param {number} [attrs.text] the additional information text
 * @param {boolean} [attrs.noratio] the hide ratio (in percent) display flag
 * @param {boolean} [attrs.novalue] the hide value display flag
 * @param {boolean} [attrs.notime] the hide remaining time display flag
 * @param {boolean} [attrs.hidden] the hidden widget flag
 */
RapidContext.Widget.ProgressBar.prototype.setAttrs = function (attrs) {
    /* eslint complexity: "off" */
    let now = new Date().getTime();
    attrs = Object.assign({}, attrs);
    if ("min" in attrs || "max" in attrs) {
        attrs.min = Math.max(parseInt(attrs.min, 10) || this.min || 0, 0);
        attrs.max = Math.max(parseInt(attrs.max, 10) || this.max || 100, attrs.min);
        attrs.value = attrs.value || null;
        attrs.ratio = attrs.ratio || 0.0;
        this.startTime = now;
        this.updateTime = now;
        this.timeRemaining = null;
    }
    if ("value" in attrs) {
        let min = attrs.min || this.min;
        let max = attrs.max || this.max;
        let val = Math.min(Math.max(parseFloat(attrs.value), min), max);
        attrs.value = isNaN(val) ? null : val;
        attrs.ratio = isNaN(val) ? null : (val - min) / (max - min);
    }
    if ("ratio" in attrs) {
        let val = Math.min(Math.max(parseFloat(attrs.ratio), 0.0), 1.0);
        attrs.ratio = isNaN(val) ? null : val;
    }
    if ("noratio" in attrs) {
        attrs.noratio = RapidContext.Data.bool(attrs.noratio) || null;
    }
    if ("novalue" in attrs) {
        attrs.novalue = RapidContext.Data.bool(attrs.novalue) || null;
    }
    if ("notime" in attrs) {
        attrs.notime = RapidContext.Data.bool(attrs.notime) || null;
        this.timeRemaining = null;
    }
    this.__setAttrs(attrs);
    if (!this.notime && now - this.updateTime > 1000) {
        let estimate = this._remainingTime();
        this.updateTime = now;
        this.timeRemaining = (estimate && estimate.text) || null;
    }
    this._render();
};

/**
 * Returns the remaining time.
 *
 * @return {Object} the remaining time object, or
 *         `null` if not possible to estimate
 */
RapidContext.Widget.ProgressBar.prototype._remainingTime = function () {
    let duration = new Date().getTime() - this.startTime;
    duration = Math.max(Math.round(duration / this.ratio - duration), 0);
    if (isFinite(duration) && !isNaN(duration)) {
        let res = {
            total: duration,
            days: Math.floor(duration / 86400000),
            hours: Math.floor(duration / 3600000) % 24,
            minutes: Math.floor(duration / 60000) % 60,
            seconds: Math.floor(duration / 1000) % 60,
            millis: duration % 1000
        };
        let pad = MochiKit.Text.padLeft;
        if (res.days >= 10) {
            res.text = res.days + " days";
        } else if (res.days >= 1) {
            res.text = res.days + " days " + res.hours + " hours";
        } else if (res.hours >= 1) {
            res.text = res.hours + ":" + pad("" + res.minutes, 2, "0") + " hours";
        } else if (res.minutes >= 1) {
            res.text = res.minutes + ":" + pad("" + res.seconds, 2, "0") + " min";
        } else {
            res.text = res.seconds + " sec";
        }
        return res;
    }
    return null;
};

/**
 * Redraws the progress bar meter and text.
 */
RapidContext.Widget.ProgressBar.prototype._render = function () {
    this.lastChild.min = this.min;
    this.lastChild.max = this.max;
    this.lastChild.value = this.value || (this.max - this.min) * this.ratio || null;
    let percent = 0;
    let info = [];
    if (!this.noratio) {
        percent = Math.round(this.ratio * 1000) / 10;
        info.push(Math.round(percent) + "%");
    }
    if (!this.novalue && typeof(this.value) == "number") {
        let pos = this.value - this.min;
        let total = this.max - this.min;
        info.push(pos + " of " + total);
    }
    if (this.text) {
        info.push(this.text);
    }
    if (this.timeRemaining && percent > 0 && percent < 100) {
        info.push(this.timeRemaining + " remaining");
    }
    this.firstChild.innerText = info.join(" \u2022 ");
};
