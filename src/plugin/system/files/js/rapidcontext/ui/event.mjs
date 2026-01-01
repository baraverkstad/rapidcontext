/*
 * RapidContext <https://www.rapidcontext.com/>
 * Copyright (c) 2007-2026 Per Cederberg. All rights reserved.
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

const listeners = [];

/**
 * Provides simplified event handling for DOM nodes. Used as a
 * mixin into RapidContext.Widget instances and similar, but also
 * provides static versions of the same functions.
 *
 * @class RapidContext.UI.Event
 */
export class Event {

    /**
     * Dispatches a single event from this DOM node. Also creates
     * a new CustomEvent instance if needed.
     *
     * @method emit
     * @memberof RapidContext.UI.Event.prototype
     * @param {string/Event} event the event type name or instance
     * @param {Object} [opts] the event options
     * @param {boolean} [opts.async=true] the async dispatch flag
     * @param {boolean} [opts.bubbles=false] the event bubbles flag
     * @param {boolean} [opts.cancelable=false] the cancellable event flag
     * @param {Object} [opts.detail] the additional event details
     * @return {boolean} `true` if event was async or not cancelled
     */
    emit(event, opts) {
        return emit(this, event, opts);
    }

    /**
     * Attaches a listener function for one or more events.
     *
     * @method on
     * @memberof RapidContext.UI.Event.prototype
     * @param {string} event the event type name (or space separated names)
     * @param {string} [selector] the CSS selector to match for event target
     * @param {function} listener the event handler function (or `false`)
     * @param {Object} [opts] the event listener options (see addEventListener)
     * @param {number} [opts.delay] an inactivity delay before calling listener
     * @return {Node} the input DOM node (for chaining calls)
     */
    on(event, selector, listener, opts) {
        return on(this, event, selector, listener, opts);
    }

    /**
     * Attaches a single event listener function. The listener will
     * be removed the first time an event is triggered.
     *
     * @method once
     * @memberof RapidContext.UI.Event.prototype
     * @param {string} event the event type name (or space separated names)
     * @param {string} [selector] the CSS selector to match for event target
     * @param {function} listener the event handler function (or `false`)
     * @param {Object} [opts] the event listener options (see addEventListener)
     * @param {number} [opts.delay] an inactivity delay before calling listener
     * @return {Node} the input DOM node (for chaining calls)
     */
    once(event, selector, listener, opts) {
        return once(this, event, selector, listener, opts);
    }

    /**
     * Removes one or more previously attached event listeners. If
     * no event details or listeners are specified, all matching
     * handlers are removed.
     *
     * @method off
     * @memberof RapidContext.UI.Event.prototype
     * @param {string} [event] the event type name (or space separated names)
     * @param {string} [selector] the CSS selector to match for event target
     * @param {function} [listener] the event handler function (or `false`)
     * @return {Node} the input DOM node (for chaining calls)
     */
    off(event, selector, listener) {
        return off(this, event, selector, listener);
    }
}

/**
 * Dispatches a single event for a DOM node. Also creates a new
 * CustomEvent instance if needed.
 *
 * @param {Node} src the DOM node emitting the event
 * @param {string/Event} event the event type name or instance
 * @param {Object} [opts] the event options
 * @param {boolean} [opts.async=true] the async dispatch flag
 * @param {boolean} [opts.bubbles=false] the event bubbles flag
 * @param {boolean} [opts.cancelable=false] the cancellable event flag
 * @param {Object} [opts.detail] the additional event details
 * @return {boolean} `true` if event was async or not cancelled
 * @function RapidContext.UI.Event.emit
 */
export function emit(src, event, opts) {
    opts = { async: true, ...opts };
    event = (event instanceof window.Event) ? event : new CustomEvent(event, opts);
    if (opts.async) {
        setTimeout(() => src.dispatchEvent(event));
        return true;
    } else {
        return src.dispatchEvent(event);
    }
}

/**
 * Attaches a listener function for one or more events.
 *
 * @param {Node} src the DOM node when event handler is attached
 * @param {string} event the event type name (or space separated names)
 * @param {string} [selector] the CSS selector to match for event target
 * @param {function} listener the event handler function (or `false`)
 * @param {Object} [opts] the event listener options (see addEventListener)
 * @param {number} [opts.delay] an inactivity delay before calling listener
 * @return {Node} the input DOM node (for chaining calls)
 * @function RapidContext.UI.Event.on
 */
export function on(src, event, selector, listener, opts) {
    if (typeof(selector) != 'string') {
        opts = arguments[3];
        listener = arguments[2];
        selector = null;
    }
    let handler = (listener === false) ? stop : listener;
    if (opts?.delay) {
        handler = debounce(opts.delay, handler);
    }
    if (selector) {
        handler = delegate.bind(null, selector, handler);
    }
    const arr = Array.isArray(event) ? event : event.split(/\s+/g);
    arr.forEach((event) => {
        src.addEventListener(event, handler, opts);
        listeners.push({ src, event, selector, listener, handler, opts });
    });
    return src;
}

/**
 * Attaches a single event listener function. The listener will
 * be removed the first time an event is triggered.
 *
 * @param {Node} src the DOM node when event handler is attached
 * @param {string} event the event type name (or space separated names)
 * @param {string} [selector] the CSS selector to match for event target
 * @param {function} listener the event handler function (or `false`)
 * @param {Object} [opts] the event listener options (see addEventListener)
 * @param {number} [opts.delay] an inactivity delay before calling listener
 * @return {Node} the input DOM node (for chaining calls)
 * @function RapidContext.UI.Event.once
 */
export function once(src, event, selector, listener, opts) {
    function handler(evt) {
        off(src, event, selector, handler);
        listener.call(this, evt);
    }
    return on(src, event, selector, handler, opts);
}

/**
 * Removes one or more previously attached event listeners. If
 * no event details or listeners are specified, all matching
 * handlers are removed.
 *
 * @param {Node} src the DOM node when event handler is attached
 * @param {string} [event] the event type name (or space separated names)
 * @param {string} [selector] the CSS selector to match for event target
 * @param {function} [listener] the event handler function (or `false`)
 * @return {Node} the input DOM node (for chaining calls)
 * @function RapidContext.UI.Event.off
 */
export function off(src, event, selector, listener) {
    if (typeof(selector) != 'string') {
        listener = arguments[2];
        selector = null;
    }
    const arr = (event == null || Array.isArray(event)) ? event : event.split(/\s+/g);
    const matches = listeners.filter((l) => {
        return src === l.src &&
               (arr == null || arr.includes(l.event)) &&
               (selector == null || selector === l.selector) &&
               (listener == null || listener === l.listener);
    });
    matches.forEach((l) => {
        src.removeEventListener(l.event, l.handler, l.opts);
        listeners.splice(listeners.indexOf(l), 1);
    });
    return src;
}

function parents(el, ancestor) {
    const path = [];
    for (; el && el !== ancestor; el = el.parentElement) {
        path.push(el);
    }
    el && path.push(el);
    return path;
}

function delegate(selector, listener, evt) {
    const isMatch = (el) => el.matches(selector);
    const forward = (el) => {
        try {
            evt.delegateTarget = el;
            listener.call(evt.currentTarget, evt);
        } finally {
            delete evt.delegateTarget;
        }
    };
    parents(evt.target, evt.currentTarget).filter(isMatch).forEach(forward);
}

function stop(evt) {
    evt.preventDefault();
    evt.stopImmediatePropagation();
}

function debounce(delay, fn, thisObj) {
    let timer;
    return function () {
        clearTimeout(timer);
        timer = setTimeout(fn.bind(thisObj ?? this, arguments), delay);
    };
}

export default Object.assign(Event, { on, once, off, emit });
