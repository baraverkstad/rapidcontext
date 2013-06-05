/*
 * RapidContext <http://www.rapidcontext.com/>
 * Copyright (c) 2007-2013 Per Cederberg. All rights reserved.
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

/**
 * @name RapidContext.Storage
 * @namespace Provides functions for accessing the server storage.
 */
(function (window, undefined) {

    // Create namespaces
    var RapidContext = window.RapidContext || (window.RapidContext = {});
    var Storage = RapidContext.Storage || (RapidContext.Storage = {});

    /**
     * Returns a storage URL for a resource. If the resource is an
     * object, its 'type' and 'id' properties will be used to form
     * the path.
     *
     * @param {String/Object} pathOrObj the path or object
     *
     * @return {String} the URL to the resource
     *
     * @throws {Error} if the object didn't have both 'type' and 'id'
     *     properties
     *
     * @private
     */
    function storageUrl(pathOrObj) {
        if (typeof(pathOrObj) != "string") {
            pathOrObj = path(pathOrObj);
        }
        if (!pathOrObj) {
            throw new Error("Invalid object or storage path");
        }
        return "rapidcontext/storage/" + pathOrObj.replace(/^\//, "");
    }

    /**
     * Returns the storage path for an object. The object must have
     * both the 'type' and 'id' properties set.
     *
     * @param {Object} obj the object to store
     *
     * @return {String} the storage path, or null if not available
     *
     * @memberof RapidContext.Storage
     */
    function path(obj) {
        return (obj && obj.type && obj.id) ? obj.type + "/" + obj.id : null;
    }

    /**
     * Reads an object on a storage path. Note that this will return
     * a JSON representation of the object, regardless of the actual
     * object type.
     *
     * @param {String/Object} pathOrObj the path or object to read
     *
     * @return {Deferred} a `MochiKit.Async.Deferred` object that will
     *         callback with the JSON data object
     *
     * @memberof RapidContext.Storage
     */
    function read(pathOrObj) {
        var url = storageUrl(pathOrObj) + ".json";
        var d = RapidContext.App.loadJSON(url, null, null);
        d.addCallback(MochiKit.Base.itemgetter("data"));
        return d;
    }

    /**
     * Writes an object to a storage path. Any previous object on the
     * specified path will be removed. If a path is specified without
     * data, only the removal is performed.
     *
     * @param {String/Object} pathOrObj the path or object to write
     * @param {Object} [data] the object to write (if path was string)
     *
     * @return {Deferred} a `MochiKit.Async.Deferred` object that will
     *         callback with the XMLHttpRequest instance on success
     *
     * @memberof RapidContext.Storage
     */
    function write(pathOrObj, data) {
        if (typeof(pathOrObj) == "string" && data == null) {
            var opts = { method: "DELETE" };
            return RapidContext.App.loadXHR(storageUrl(pathOrObj), null, opts);
        } else {
            var json = JSON.stringify(data || pathOrObj);
            var headers = { "Content-Type": "application/json" };
            var opts = { method: "POST", sendContent: json, headers: headers };
            return RapidContext.App.loadXHR(storageUrl(pathOrObj), null, opts);
        }
    }

    /**
     * Updates an object with properties from a partial object. The
     * properties in the partial object will overwrite any previous
     * properties with the same name in the destination object. No
     * merging of property values will be performed.
     *
     * @param {String/Object} pathOrObj the path or object to write
     * @param {Object} data the partial object properties
     *
     * @return {Deferred} a `MochiKit.Async.Deferred` object that will
     *         callback with the updated data object on success
     *
     * @memberof RapidContext.Storage
     */
    function update(pathOrObj, data) {
        var json = JSON.stringify(data);
        var headers = { "Content-Type": "application/json" };
        var opts = { method: "PATCH", sendContent: json, headers: headers };
        return RapidContext.App.loadJSON(storageUrl(pathOrObj), null, opts);
    }

    // Export symbols
    Storage.path = path;
    Storage.read = read;
    Storage.write = write;
    Storage.update = update;

})(this);
