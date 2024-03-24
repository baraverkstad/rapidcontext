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

/**
 * Provides functions for accessing the server storage (BETA).
 * @namespace RapidContext.Storage
 */
(function (window) {

    /**
     * Returns a storage URL for a resource. If the resource is an
     * object, its 'type' and 'id' properties will be used to form
     * the path.
     *
     * @param {string|Object} pathOrObj the path or object
     *
     * @return {string} the URL to the resource
     *
     * @throws {Error} if the object didn't have both 'type' and 'id'
     *     properties
     *
     * @private
     */
    function storageUrl(pathOrObj) {
        var ident = (typeof(pathOrObj) === "string") ? pathOrObj : path(pathOrObj);
        if (!ident) {
            throw new Error("Invalid object or storage path");
        }
        return "rapidcontext/storage/" + ident.replace(/^\//, "");
    }

    /**
     * Returns the storage path for an object. The object must have
     * both the 'type' and 'id' properties set.
     *
     * @param {Object} obj the object to store
     *
     * @return {string} the storage path, or null if not available
     *
     * @memberof RapidContext.Storage
     */
    function path(obj) {
        var type = obj && obj.type && obj.type.split("/")[0];
        return (type && obj.id) ? type + "/" + obj.id : null;
    }

    /**
     * Reads an object on a storage path. Note that this will return
     * a JSON representation of the object, regardless of the actual
     * object type.
     *
     * @param {string|Object} pathOrObj the path or object to read
     *
     * @return {Promise} a `RapidContext.Async` promise that will
     *         resolve with the object data
     *
     * @memberof RapidContext.Storage
     */
    function read(pathOrObj) {
        var url = storageUrl(pathOrObj);
        url += url.endsWith("/") ? "index.json" : ".json";
        return RapidContext.App.loadJSON(url, null, null);
    }

    /**
     * Writes an object to a storage path. Any previous object on the
     * specified path will be removed. If a path is specified without
     * data, only the removal is performed.
     *
     * @param {string|Object} pathOrObj the path or object to write
     * @param {Object} [data] the object to write (if path was string)
     *
     * @return {Promise} a `RapidContext.Async` promise that will
     *         resolve when the object has been written
     *
     * @memberof RapidContext.Storage
     */
    function write(pathOrObj, data) {
        var url = storageUrl(pathOrObj);
        if (typeof(pathOrObj) == "string" && data == null) {
            return RapidContext.App.loadXHR(url, null, { method: "DELETE" });
        } else {
            var headers = { "Content-Type": "application/json" };
            var opts = { method: "POST", headers: headers };
            return RapidContext.App.loadXHR(url + ".yaml", data || pathOrObj, opts);
        }
    }

    /**
     * Updates an object with properties from a partial object. The
     * properties in the partial object will overwrite any previous
     * properties with the same name in the destination object. No
     * merging of property values will be performed.
     *
     * @param {string|Object} pathOrObj the path or object to write
     * @param {Object} [data] the partial object (changes) to write
     *
     * @return {Promise} a `RapidContext.Async` promise that will
     *         resolve with the updated data object on success
     *
     * @memberof RapidContext.Storage
     */
    function update(pathOrObj, data) {
        var url = storageUrl(pathOrObj);
        var newPath = path(data);
        var headers = { "Content-Type": "application/json" };
        if (newPath && newPath != path(pathOrObj)) {
            headers["X-Move-To"] = newPath + ".yaml";
        }
        var opts = { method: "PATCH", headers: headers };
        return RapidContext.App.loadJSON(url, data || pathOrObj, opts);
    }

    // Create namespaces & export symbols
    var RapidContext = window.RapidContext || (window.RapidContext = {});
    var Storage = RapidContext.Storage || (RapidContext.Storage = {});
    Object.assign(Storage, { path, read, write, update });

})(this);
