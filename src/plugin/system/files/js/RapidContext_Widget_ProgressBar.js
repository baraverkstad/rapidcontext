/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2023 Per Cederberg. All rights reserved.
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
 * @param {Number} [attrs.min] the minimum range value, defaults to 0
 * @param {Number} [attrs.max] the maximum range value, defaults to 100
 * @param {Number} [attrs.value] the progress value, a number between `min`
 *            and `max`, defaults to 0
 * @param {Number} [attrs.ratio] the progress ratio, a floating-point number
 *            between 0.0 and 1.0, defaults to 0.0
 * @param {Number} [attrs.text] the additional information text, defaults to
 *            blank
 * @param {Boolean} [attrs.noratio] the hide ratio (in percent) display flag,
 *            defaults to `false`
 * @param {Boolean} [attrs.novalue] the hide value display flag, defaults to
 *            `false`
 * @param {Boolean} [attrs.notime] the hide remaining time display flag,
 *            defaults to `false`
 * @param {Boolean} [attrs.hidden] the hidden widget flag, defaults to false
 *
 * @return {Widget} the widget DOM node
 *
 * @class The progress bar widget class. Used to provide a dynamic progress
 *     meter, using a `<div>` HTML elements. The progress bar also provides a
 *     completion time estimation that is displayed in the bar. Whenever the
 *     range is modified, the time estimation is reset.
 * @extends RapidContext.Widget
 *
 * @example {JavaScript}
 * var attrs = { text: "Working", noratio: true, notime: true };
 * var w = RapidContext.Widget.ProgressBar(attrs);
 *
 * @example {User Interface XML}
 * <ProgressBar text="Working" noratio="true" notime="true" />
 */
RapidContext.Widget.ProgressBar = function (attrs) {
    var meter = MochiKit.DOM.DIV({ "class": "widgetProgressBarMeter" });
    var text = MochiKit.DOM.DIV({ "class": "widgetProgressBarText" });
    var o = MochiKit.DOM.DIV({}, meter, text);
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
 * @param {Number} [attrs.min] the minimum range value, if modified the current
 *            value and ratio are reset
 * @param {Number} [attrs.max] the maximum range value, if modified the current
 *            value and ratio are reset
 * @param {Number} [attrs.value] the progress value, a number between `min`
 *            and `max`
 * @param {Number} [attrs.ratio] the progress ratio, a floating-point number
 *            between 0.0 and 1.0
 * @param {Number} [attrs.text] the additional information text
 * @param {Boolean} [attrs.noratio] the hide ratio (in percent) display flag
 * @param {Boolean} [attrs.novalue] the hide value display flag
 * @param {Boolean} [attrs.notime] the hide remaining time display flag
 * @param {Boolean} [attrs.hidden] the hidden widget flag
 */
RapidContext.Widget.ProgressBar.prototype.setAttrs = function (attrs) {
    attrs = Object.assign({}, attrs);
    var nowTime = new Date().getTime();
    if (typeof(attrs.min) != "undefined" || typeof(attrs.max) != "undefined") {
        this.min = attrs.min = Math.max(parseInt(attrs.min) || this.min || 0, 0);
        this.max = attrs.max = Math.max(parseInt(attrs.max) || this.max || 100, this.min);
        attrs.value = attrs.value || null;
        attrs.ratio = attrs.ratio || 0.0;
        this.startTime = nowTime;
        this.updateTime = nowTime;
        this.timeRemaining = null;
    }
    if (typeof(attrs.value) != "undefined") {
        var value = Math.min(Math.max(parseFloat(attrs.value), this.min), this.max);
        if (!isNaN(value)) {
            attrs.value = value;
            var calc = (value - this.min) / (this.max - this.min);
            attrs.ratio = attrs.ratio || calc;
        } else {
            delete attrs.value;
        }
    }
    if (typeof(attrs.ratio) != "undefined") {
        var ratio = Math.min(Math.max(parseFloat(attrs.ratio), 0.0), 1.0);
        if (!isNaN(ratio)) {
            attrs.ratio = ratio;
        } else {
            delete attrs.ratio;
        }
    }
    if (typeof(attrs.noratio) != "undefined") {
        attrs.noratio = MochiKit.Base.bool(attrs.noratio) || null;
    }
    if (typeof(attrs.novalue) != "undefined") {
        attrs.novalue = MochiKit.Base.bool(attrs.novalue) || null;
    }
    if (typeof(attrs.notime) != "undefined") {
        attrs.notime = MochiKit.Base.bool(attrs.notime) || null;
        this.timeRemaining = null;
    }
    this.__setAttrs(attrs);
    if (!this.notime && nowTime - this.updateTime > 1000) {
        this.updateTime = nowTime;
        var time = this._remainingTime();
        this.timeRemaining = (time && time.text) ? time.text : null;
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
    var duration = new Date().getTime() - this.startTime;
    duration = Math.max(Math.round(duration / this.ratio - duration), 0);
    if (isFinite(duration) && !isNaN(duration)) {
        var res = {
            total: duration,
            days: Math.floor(duration / 86400000),
            hours: Math.floor(duration / 3600000) % 24,
            minutes: Math.floor(duration / 60000) % 60,
            seconds: Math.floor(duration / 1000) % 60,
            millis: duration % 1000
        };
        var pad = MochiKit.Text.padLeft;
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
    var percent = Math.round(this.ratio * 1000) / 10;
    MochiKit.Style.setElementDimensions(this.firstChild, { w: percent }, "%");
    if (percent < 100) {
        this.firstChild.className = "widgetProgressBarMeter animated";
    } else {
        this.firstChild.className = "widgetProgressBarMeter";
    }
    var info = [];
    if (!this.noratio) {
        info.push(Math.round(percent) + "%");
    }
    if (!this.novalue && typeof(this.value) == "number") {
        var pos = this.value - this.min;
        var total = this.max - this.min;
        info.push(pos + " of " + total);
    }
    if (this.text) {
        info.push(this.text);
    }
    if (this.timeRemaining && percent < 100) {
        info.push(this.timeRemaining + " remaining");
    }
    MochiKit.DOM.replaceChildNodes(this.lastChild, info.join(" \u2022 "));
};
