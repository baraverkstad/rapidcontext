/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2012 Per Cederberg. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the BSD license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the RapidContext LICENSE.txt file for more details.
 */

// Namespace initialization
if (typeof(RapidContext) == "undefined") {
    RapidContext = {};
}
RapidContext.Widget = RapidContext.Widget || { Classes: {}};

/**
 * Creates a new progress bar widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {Number} [attrs.min] the minimum range value, defaults to 0
 * @param {Number} [attrs.max] the maximum range value, defaults to 100
 * @param {Number} [attrs.value] the progress value, a number between min
 *            and max, defaults to 0
 * @param {Number} [attrs.ratio] the progress ratio, a floating-point number
 *            between 0.0 and 1.0, defaults to 0.0
 * @param {Number} [attrs.text] the additional information text, defaults to
 *            blank
 * @param {Boolean} [attrs.noratio] the hide ratio (in percent) display flag,
 *            defaults to false
 * @param {Boolean} [attrs.novalue] the hide value display flag, defaults to
 *            false
 * @param {Boolean} [attrs.notime] the hide remaining time display flag,
 *            defaults to false
 *
 * @return {Widget} the widget DOM node
 *
 * @class The progress bar widget class. Used to provide a dynamic
 *     progress meter, using a &lt;div&gt; HTML elements. The
 *     progress bar also provides a completion time estimation that
 *     is displayed in the bar. Whenever the range is modified, the
 *     time estimation is reset.
 * @extends RapidContext.Widget
 *
 * @example {JavaScript}
 * var attrs = { text: "Working", noratio: true, notime: true };
 * var w = RapidContext.Widget.ProgressBar(attrs);
 *
 * @example {User Interface XML}
 * <ProgressBar text="Working" noratio="1" notime="1" />
 */
RapidContext.Widget.ProgressBar = function (attrs) {
    var meter = MochiKit.DOM.DIV({ "class": "widgetProgressBarMeter" });
    var text = MochiKit.DOM.DIV({ "class": "widgetProgressBarText" });
    var o = MochiKit.DOM.DIV({}, meter, text);
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.addClass("widgetProgressBar");
    o.setAttrs(MochiKit.Base.update({ min: 0, max: 100, value: 0 }, attrs));
    return o;
};

// Register widget class
RapidContext.Widget.Classes.ProgressBar = RapidContext.Widget.ProgressBar;

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
 * @param {Number} [attrs.value] the progress value, a number between min
 *            and max
 * @param {Number} [attrs.ratio] the progress ratio, a floating-point number
 *            between 0.0 and 1.0
 * @param {Number} [attrs.text] the additional information text
 * @param {Boolean} [attrs.noratio] the hide ratio (in percent) display flag
 * @param {Boolean} [attrs.novalue] the hide value display flag
 * @param {Boolean} [attrs.notime] the hide remaining time display flag
 * */
RapidContext.Widget.ProgressBar.prototype.setAttrs = function (attrs) {
    attrs = MochiKit.Base.update({}, attrs);
    var nowTime = new Date().getTime();
    if (typeof(attrs.min) != "undefined" || typeof(attrs.max) != "undefined") {
        this.min = attrs.min = Math.max(parseInt(attrs.min) || this.min || 0, 0);
        this.max = attrs.max = Math.max(parseInt(attrs.max) || this.max || 100, this.min);
        this.value = null;
        this.ratio = 0.0;
        this.startTime = nowTime;
        this.updateTime = nowTime;
        this.timeRemaining = null;
    }
    if (typeof(attrs.value) != "undefined") {
        var value = Math.min(Math.max(parseFloat(attrs.value), this.min), this.max);
        if (!isNaN(value)) {
            this.value = attrs.value = value;
            this.ratio = (this.value - this.min) / (this.max - this.min);
        } else {
            delete attrs.value;
        }
    }
    if (typeof(attrs.ratio) != "undefined") {
        var ratio = Math.min(Math.max(parseFloat(attrs.ratio), 0.0), 1.0);
        if (!isNaN(ratio)) {
            this.ratio = attrs.ratio = ratio;
        } else {
            delete attrs.ratio;
        }
    }
    if (typeof(attrs.text) != "undefined") {
        this.text = attrs.text;
    }
    if (typeof(attrs.noratio) != "undefined") {
        this.noratio = MochiKit.Base.bool(attrs.noratio);
    }
    if (typeof(attrs.novalue) != "undefined") {
        this.novalue = MochiKit.Base.bool(attrs.novalue);
    }
    if (typeof(attrs.notime) != "undefined") {
        this.notime = MochiKit.Base.bool(attrs.notime);
        this.timeRemaining = null;
    }
    this.__setAttrs(attrs);
    if (!this.notime && nowTime - this.updateTime > 1000) {
        this.updateTime = nowTime;
        var duration = nowTime - this.startTime;
        duration = Math.max(Math.round(duration / this.ratio - duration), 0);
        if (isFinite(duration) && !isNaN(duration)) {
            this.timeRemaining = RapidContext.Util.toApproxPeriod(duration);
        } else {
            this.timeRemaining = null;
        }
    }
    this._render();
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
    MochiKit.DOM.replaceChildNodes(this.lastChild, info.join(" \u2014 "));
};
