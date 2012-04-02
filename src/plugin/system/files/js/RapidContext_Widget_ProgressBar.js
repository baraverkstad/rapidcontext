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
RapidContext.Widget = RapidContext.Widget || { Classes: {}};

/**
 * Creates a new progress bar widget.
 *
 * @constructor
 * @param {Object} attrs the widget and node attributes
 * @param {Number} [attrs.min] the minimum range value, defaults to 0
 * @param {Number} [attrs.max] the maximum range value, defaults to 100
 *
 * @return {Widget} the widget DOM node
 *
 * @class The progress bar widget class. Used to provide a dynamic
 *     progress meter, using two &lt;div&gt; HTML elements. The
 *     progress bar also provides a completion time estimation that
 *     is displayed in the bar. Whenever the range is modified, the
 *     time estimation is reset.
 * @extends RapidContext.Widget
 */
RapidContext.Widget.ProgressBar = function (attrs) {
    var meter = MochiKit.DOM.DIV({ "class": "widgetProgressBarMeter" });
    var text = MochiKit.DOM.DIV({ "class": "widgetProgressBarText" });
    var o = MochiKit.DOM.DIV({}, meter, text);
    RapidContext.Widget._widgetMixin(o, arguments.callee);
    o.addClass("widgetProgressBar");
    o.setAttrs(MochiKit.Base.update({ min: 0, max: 100 }, attrs));
    o.setValue(0);
    return o;
};

// Register widget class
RapidContext.Widget.Classes.ProgressBar = RapidContext.Widget.ProgressBar;

/**
 * Updates the widget or HTML DOM node attributes.
 *
 * @param {Object} attrs the widget and node attributes to set
 * @param {Number} [attrs.min] the minimum range value, defaults to 0
 * @param {Number} [attrs.max] the maximum range value, defaults to 100
 */
RapidContext.Widget.ProgressBar.prototype.setAttrs = function (attrs) {
    attrs = MochiKit.Base.update({}, attrs);
    var locals = RapidContext.Util.mask(attrs, ["min", "max"]);
    if (typeof(locals.min) != "undefined" || typeof(locals.max) != "undefined") {
        this.minValue = parseInt(locals.min) || 0;
        this.maxValue = parseInt(locals.max) || 100;
        this.startTime = new Date().getTime();
        this.lastTime = this.startTime;
        this.timeLeft = null;
    }
    MochiKit.DOM.updateNodeAttributes(this, attrs);
};

/**
 * Updates the progress bar completion value. The value should be
 * within the range previuosly established by the "min" and "max"
 * attributes and is used to calculate a completion ratio. The
 * ratio is then used both for updating the progress meter and for
 * calculating an approximate remaining time. Any previous progress
 * bar text will be replaced by this method.
 *
 * @param {Number} value the new progress value
 * @param {String} [text] the additional information text
 */
RapidContext.Widget.ProgressBar.prototype.setValue = function (value, text) {
    value = Math.min(Math.max(value, this.minValue), this.maxValue);
    var pos = value - this.minValue;
    var total = this.maxValue - this.minValue;
    var str = pos + " of " + total;
    if (typeof(text) == "string" && text != "") {
        str += " \u2014 " + text;
    }
    this.setRatio(pos / total, str);
};

/**
 * Updates the progress bar completion ratio. The ratio value should
 * be a floating-point number between 0.0 and 1.0. The ratio is used
 * both for updating the progress meter and for calculating an
 * approximate remaining time. Any previous progress bar text will
 * be replaced by this method.
 *
 * @param {Number} ratio the new progress ratio, a floating-point number
 *            between 0.0 and 1.0
 * @param {String} [text] the additional information text
 */
RapidContext.Widget.ProgressBar.prototype.setRatio = function (ratio, text) {
    var percent = Math.round(ratio * 1000) / 10;
    MochiKit.Style.setElementDimensions(this.firstChild, { w: percent }, "%");
    if (percent < 100) {
        this.firstChild.className = "widgetProgressBarMeter animated";
    } else {
        this.firstChild.className = "widgetProgressBarMeter";
    }
    if (typeof(text) == "string" && text != "") {
        text = Math.round(percent) + "% \u2014 " + text;
    } else {
        text = Math.round(percent) + "%";
    }
    var nowTime = new Date().getTime();
    if (nowTime - this.lastTime > 1000) {
        this.lastTime = nowTime;
        var period = nowTime - this.startTime;
        period = Math.max(Math.round(period / ratio - period), 0);
        this.timeLeft = RapidContext.Util.toApproxPeriod(period);
    }
    if (this.timeLeft != null && percent > 0 && percent < 100) {
        text += " \u2014 " + this.timeLeft + " left";
    }
    this.setText(text);
};

/**
 * Updates the progress bar text.
 *
 * @param {String} text the new progress bar text
 */
RapidContext.Widget.ProgressBar.prototype.setText = function (text) {
    MochiKit.DOM.replaceChildNodes(this.lastChild, text);
};
