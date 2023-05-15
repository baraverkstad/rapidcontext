/**
 * @namespace Global object utilities object.
 */
ObjectUtil = {};

/**
 * Searches for an object property value.
 *
 * @param obj                the object to search
 * @param value              the property value
 *
 * @return the object property name, or
 *         null if not found
 */
ObjectUtil.indexOf = function(obj, value) {
    if (obj != null) {
        for (var name in obj) {
            if (obj[name] == value) {
                 return name;
            }
        }
    }
    return null;
}

/**
 * Applies a function to each property in an object and returns a
 * new object. The new object will contain the same property names
 * but the values will contain the result of the function call. Note
 * that the order in which the properties are processed is undefined.
 *
 * @param func               the function to execute
 * @param obj                the object with input data
 *
 * @return a new object with the results
 */
ObjectUtil.map = function(func, obj) {
    var res = {};

    for (var name in obj) {
        res[name] = func.call(null, obj[name], name, obj);
    }
    return res;
}

/**
 * Applies an accumulative function to each property in an object
 * and returns the result. The result from each function call will
 * be used as the first input value in the subsequent call, until
 * all properties have been used. The initial value will be used in
 * the first call to the function. Note that the order in which the
 * properties are processed is undefined.
 *
 * @param func               the function to execute
 * @param initial            the initial or seed value
 * @param obj                the object with input data
 *
 * @return the result from the last function execution, or
 *         the initial value if the object was empty
 */
ObjectUtil.reduce = function(func, initial, obj) {
    var res = initial;

    for (var name in obj) {
        res = func.call(null, res, obj[name], name, obj);
    }
    return res;
}

/**
 * Retrieves a nested property from an object. The property name may
 * contain any number of dot (".") characters, which will be resolved
 * to the respective subobject. If one or more of the properties is
 * an array, all the property values will be extracted and returned
 * in a result array.
 *
 * @param obj                the data object
 * @param name               the property name (or path)
 * @param defval             the default value
 *
 * @return the property value found, or
 *         an array with all property values found, or
 *         the default value if the property didn't exist
 */
ObjectUtil.get = function(obj, name, defval) {
    var path = name.split(".");

    for (var i = 0; i < path.length; i++) {
        if (obj == null) {
            return defval;
        } else if (obj instanceof Array) {
            var res = [];
            name = path.slice(i).join(".");
            for (var i = 0; i < obj.length; i++) {
                res.push(ObjectUtil.get(obj[i], name, defval));
            }
            return res;
        } else if (obj.hasOwnProperty(path[i])) {
            obj = obj[path[i]];
        } else {
            return defval;
        }
    }
    return obj;
}

/**
 * Merges two values into one according to a number of rules:
 *
 * 1. If one of the two values is null or undefined, the other
 *    values is returned.
 *
 * 2. If one or both values are arrays, the result will be a new
 *    array containing the elements of both arrays. Any non array
 *    value will then be added to the resulting array.
 *
 * 3. If both values are objects, the result will be a new object
 *    containing the properties of both objects. If the same
 *    properties exist in both objects, their respective values will
 *    be added to an array.
 *
 * 4. Otherwise a new array will be returned containing the two
 *    values.
 *
 * @param obj1               the first value
 * @param obj2               the second value
 *
 * @return the merged values
 */
ObjectUtil.merge = function(obj1, obj2) {
    if (obj1 == null) {
        return obj2;
    } else if (obj2 == null) {
        return obj1;
    } else if (obj1 instanceof Array) {
        return obj1.concat(obj2);
    } else if (obj2 instanceof Array) {
        return [obj1].concat(obj2)
    } else if (typeof obj1 == "object" && typeof obj2 == "object") {
        var res = {};
        for (var name in obj1) {
            res[name] = obj1[name];
        }
        for (var name in obj2) {
            ObjectUtil.mergeProperty(res, name, obj2[name]);
        }
        return res;
    } else {
        return [obj1, obj2];
    }
}

/**
 * Merges a property name and value into an object. The merging is
 * destructive, as the original object is modified by this function.
 *
 * @param obj                the object to merge with
 * @param name               the property name
 * @param value              the property value
 *
 * @return the object merged with
 */
ObjectUtil.mergeProperty = function(obj, name, value) {
    if (obj[name] instanceof Array) {
        obj[name] = obj[name].concat(value);
    } else if (obj.hasOwnProperty(name)) {
        obj[name] = [obj[name], value];
    } else {
        obj[name] = value;
    }
    return obj;
}

/**
 * Flattens a nested object structure. All object properties and
 * array elements will be flattened and then merged into a single
 * object.
 *
 * @param obj                the object to flatten
 *
 * @return the flattened data object
 */
ObjectUtil.flatten = function(obj) {
    if (obj instanceof Array) {
        obj = ArrayUtil.map(ObjectUtil.flatten, obj);
        obj = ArrayUtil.reduce(ObjectUtil.merge, null, obj);
    } else if (obj != null && typeof obj == "object") {
        obj = ObjectUtil.map(ObjectUtil.flatten, obj);
        obj = ObjectUtil.reduce(ObjectUtil._mergeFlatten, {}, obj);
    }
    return obj;
}

/**
 * Merges a property value while also flattening the resulting object
 * structure.
 *
 * @param obj                the initial object
 * @param value              the property value
 * @param name               the property name
 *
 * @return the merged object
 *
 * @private
 */
ObjectUtil._mergeFlatten = function(obj, value, name) {
    if (value != null && typeof value == "object") {
        return ObjectUtil.merge(obj, value);
    } else {
        ObjectUtil.mergeProperty(obj, name, value);
        return obj;
    }
}


/**
 * @namespace Global function utilities object.
 */
FunctionUtil = {};

/**
 * Returns a new function bound to the specified object. I.e.
 * whenever the returned function is called, the specified
 * function will be called with the 'this' object set to the
 * value of 'self'. Additional arguments to this function
 * will also be bound to the function call.
 *
 * If the function name is specified as a string, it will be looked
 * up in the object upon each call. This is useful to allow the
 * object function to be replaced after the call to bind().
 *
 * @param func               the function or name to bind
 * @param obj                the static 'this' object, or
 *                           null to use real 'this'
 * @param ...                optional arguments are passed
 *                           before call arguments
 *
 * @return a new bound function
 */
FunctionUtil.bind = function(func, obj) {
    var resFunc;

    if (typeof(func) == "string" && obj == null) {
        throw new Error("cannot bind named function to null object");
    } else if (typeof(func) != "string" && typeof(func) != "function") {
        throw new Error("cannot bind non-function");
    }
    resFunc = function() {
        var self = arguments.callee;
        var selfFunc = self.bindFunc;
        var selfObj = self.bindObj;
        var selfArgs = arguments;
        if (typeof(selfObj) == "undefined") {
            selfObj = this;
        }
        if (typeof(selfFunc) == "string") {
            selfFunc = selfObj[selfFunc];
        }
        if (self.bindArgs.length > 0) {
            selfArgs = self.bindArgs.concat.apply(self.bindArgs, selfArgs);
        }
        return selfFunc.apply(selfObj, selfArgs);
    }
    resFunc.bindFunc = func;
    resFunc.bindObj = obj;
    resFunc.bindArgs = Array.prototype.slice.call(arguments, 2);
    return resFunc;
}


/**
 * @namespace Global string utilities object.
 */
StringUtil = {};

/**
 * Counts the number of occurrencies of the specified substring.
 *
 * @param str                the string to check
 * @param substr             the substring to search for
 *
 * @return the number of occurrencies found, or
 *         zero (0) if not found
 */
StringUtil.count = function(str, substr) {
    var count = 0;
    var pos = 0;

    str = str || "";
    while (substr != null && (pos = str.indexOf(substr, pos)) >= 0) {
        pos++;
        count++;
    }
    return count;
}

/**
 * Checks if a string starts with the specified substring.
 *
 * @param str                the string to check
 * @param substr             the substring to search for
 *
 * @return true if the string starts with the substring, or
 *         false otherwise
 */
StringUtil.startsWith = function(str, substr) {
    return str != null && substr != null && str.indexOf(substr) == 0;
}

/**
 * Checks if a string ends with the specified substring.
 *
 * @param str                the string to check
 * @param substr             the substring to search for
 *
 * @return true if the string ends with the substring, or
 *         false otherwise
 */
StringUtil.endsWith = function(str, substr) {
    return str != null && substr != null &&
           str.indexOf(substr) == Math.max(str.length - substr.length, 0);
}

/**
 * Checks if a string contains the specified substring.
 *
 * @param str                the string to check
 * @param substr             the substring to search for
 *
 * @return true if the string contains the substring, or
 *         false otherwise
 */
StringUtil.contains = function(str, substr) {
    return substr == null ||
           (str != null && str.indexOf(substr) >= 0);
}

/**
 * Removes leading and trailing whitespace from each line in a
 * string.
 *
 * @param str                the string to trim
 *
 * @return the trimmed string, or
 *         an empty string if the string was null
 */
StringUtil.trimLines = function(str) {
    str = str || "";
    var lines = str.split("\n");
    for (var i = 0; i < lines.length; i++) {
        lines[i] = MochiKit.Format.strip(lines[i]);
    }
    return lines.join("\n");
}

/**
 * Converts a string to a camel-cased string, i.e. all spaces, hyphens
 * and underscores will be removed and the subsequent character will
 * be uppercase.
 *
 * @param str                the string to convert
 *
 * @return the converted string
 */
StringUtil.toCamelCase = function(str) {
    return str.replace(/[ _-][a-z]/g, StringUtil._toCamelCase);
}

/**
 * Fixes a string match to camel case.
 *
 * @param match              the matching string
 *
 * @return the uppercase string without the first character
 *
 * @private
 */
StringUtil._toCamelCase = function(match) {
    return match.substring(1).toUpperCase();
}

/**
 * Converts a string to a word-cased string, i.e. all words are
 * separated with spaces and each word starts with a capitilized
 * letter.
 *
 * @param str                the string to convert
 *
 * @return the converted string
 */
StringUtil.toWordCase = function(str) {
    str = str.replace(/[a-z _-][A-Z]/g, StringUtil._toWordCase);
    return str.substring(0, 1).toUpperCase() + str.substring(1);
}

/**
 * Fixes a string match to word case.
 *
 * @param match              the matching string
 *
 * @return the proper word case string
 *
 * @private
 */
StringUtil._toWordCase = function(match) {
    var res = "";
    var first = match.substring(0, 1);
    if (first != " " && first != "_" && first != "-") {
        res += first;
    }
    return res + " " + match.substring(1).toUpperCase();
}



/**
 * @namespace Global array utilities object.
 */
ArrayUtil = {};

/**
 * Searches for an array element.
 *
 * @param array              the array to search
 * @param value              the element to search for
 *
 * @return the array index, or
 *         -1 if not found
 */
ArrayUtil.indexOf = function(array, value) {
    for (var i = 0; array != null && i < array.length; i++) {
        if (array[i] == value) {
            return i;
        }
    }
    return -1;
}

/**
 * Searches for an array element having the specified property
 * value.
 *
 * @param array              the array to search
 * @param name               the object property name
 * @param value              the property value to search for
 *
 * @return the array index, or
 *         -1 if not found
 */
ArrayUtil.find = function(array, name, value) {
    for (var i = 0; array != null && i < array.length; i++) {
        if (array[i] != null && array[i][name] == value) {
            return i;
        }
    }
    return -1;
}

/**
 * Adds an element to an array if it is not already present.
 *
 * @param array              the array to modify
 * @param elem               the element to add
 *
 * @return the array object
 */
ArrayUtil.addUnique = function(array, elem) {
    if (ArrayUtil.indexOf(array, elem) < 0) {
        array.push(elem);
    }
    return array;
}

/**
 * Removes an element from an array by index.
 *
 * @param array              the array to modify
 * @param index              the element index
 *
 * @return the array object
 */
ArrayUtil.removeIndex = function(array, index) {
    array.splice(index, 1);
    return array;
}

/**
 * Removes a specified element from an array.
 *
 * @param array              the array to modify
 * @param elem               the element to remove
 *
 * @return the array object
 */
ArrayUtil.removeElem = function(array, elem) {
    for (var i = 0; i < array.length; i++) {
        if (array[i] == elem) {
            array.splice(i, 1);
            i--;
        }
    }
    return array;
}

/**
 * Removes the first element from an array.
 *
 * @param array              the array to modify
 *
 * @return the element removed, or
 *         null if no such element existed
 */
ArrayUtil.removeFirst = function(array) {
    var elem = null;

    if (array.length > 0) {
        elem = array[0];
        array.splice(0, 1);
    }
    return elem;
}

/**
 * Removes the last element from an array.
 *
 * @param array              the array to modify
 *
 * @return the element removed, or
 *         null if no such element existed
 */
ArrayUtil.removeLast = function(array) {
    return (array.length > 0) ? array.pop() : null;
}

/**
 * Sorts an array with respect to the natural order of the specified
 * object property. The array must contain objects with the property
 * and will be sorted in-place, i.e. the array might be modified.
 *
 * @param array             the array to sort
 * @param name              the property name
 *
 * @return the input array
 */
ArrayUtil.sort = function(array, name) {
    if (array != null) {
        array.sort(function(obj1, obj2) {
            var value1 = (obj1 == null) ? null : obj1[name];
            var value2 = (obj2 == null) ? null : obj2[name];
            if (value1 == null && value2 == null) {
                return 0;
            } else if (value1 == null) {
                return -1;
            } else if (value2 == null) {
                return 1;
            } else if (value1 == value2) {
                return 0;
            } else if (value1 < value2) {
                return -1;
            } else {
                return 1;
            }
        });
    }
    return array;
}

/**
 * Creates a new array without any duplicate values from the
 * specified array.
 *
 * @param array              the array to filter
 *
 * @return a new filtered array
 */
ArrayUtil.unique = function(array) {
    return ArrayUtil.reduce(ArrayUtil.addUnique, [], array);
}

/**
 * Applies a function to each element in an array and returns an
 * array with the results.
 *
 * @param func               the function to execute
 * @param array              the array with input data
 *
 * @return an array with the results
 */
ArrayUtil.map = function(func, array) {
    var res = [];

    for (var i = 0; array != null && i < array.length; i++) {
        res.push(func.call(null, array[i], i, array));
    }
    return res;
}

/**
 * Applies an accumulative function to each element in an array and
 * returns the result. The result from each function call will
 * be used as the first input value in the subsequent call, until
 * all properties have been used. The initial value will be used in
 * the first call to the function.
 *
 * @param func               the function to execute
 * @param initial            the initial or seed value
 * @param array              the array with input data
 *
 * @return the result from the last function execution, or
 *         the initial value if the array was empty
 */
ArrayUtil.reduce = function(func, initial, array) {
    var res = initial;

    for (var i = 0; array != null && i < array.length; i++) {
        res = func.call(null, res, array[i], i, array);
    }
    return res;
}


/**
 * @namespace Global date utilities object.
 */
DateUtil = {
    MILLIS_PER_SECOND: 1000,
    MILLIS_PER_MINUTE: 60 * 1000,
    MILLIS_PER_HOUR: 60 * 60 * 1000,
    MILLIS_PER_DAY: 24 * 60 * 60 * 1000,
    MILLIS_PER_WEEK: 7 * 24 * 60 * 60 * 1000,
    twoDigits: MochiKit.Format.numberFormatter("00")
};

/**
 * Converts a number of milliseconds to an approximate time period.
 *
 * @param millis             the number of milliseconds
 *
 * @return the approximate time period (as a string)
 */
DateUtil.toApproxPeriod = function(millis) {
    var days = Math.floor(millis / DateUtil.MILLIS_PER_DAY);
    var hours = Math.floor(millis / DateUtil.MILLIS_PER_HOUR) % 24;
    var mins = Math.floor(millis / DateUtil.MILLIS_PER_MINUTE) % 60;
    var secs = Math.floor(millis / DateUtil.MILLIS_PER_SECOND) % 60;

    if (days >= 10) {
        return days + " days";
    } else if (days >= 1) {
        return days + " days " + DateUtil.twoDigits(hours) + " hours";
    } else if (hours >= 1) {
        return hours + ":" + DateUtil.twoDigits(mins) + " hours";
    } else if (mins >= 1) {
        return mins + ":" + DateUtil.twoDigits(secs) + " minutes";
    } else if (secs >= 1) {
        return secs + " seconds";
    } else {
        return millis + " milliseconds";
    }
}


/**
 * @namespace Global HTML CSS utilities object.
 */
CssUtil = {};

/**
 * Resizes a DOM node based on previously stored resize values. The
 * values are stored as strings to be evaluated in the DOM nodes.
 * This function will recursively compute all child nodes as well.
 *
 * @param node               the HTML DOM node
 */
CssUtil.resize = ReTracer.Util.resizeElements;

/**
 * Sets a single style property on a DOM node.
 *
 * @param node               the HTML DOM node
 * @param name               the CSS property name
 * @param value              the CSS property value
 */
CssUtil.setStyle = function(node, name, value) {
    name = StringUtil.toCamelCase(name);
    if (value != null && typeof(value) != "string") {
        value = "" + value;
    }
    switch (name) {
    case "w":
        node.styleW = value;
        ReTracer.Util.registerSizeConstraints(node, node.styleW, node.styleH);
        break;
    case "h":
        node.styleH = value;
        ReTracer.Util.registerSizeConstraints(node, node.styleW, node.styleH);
        break;
    case "class":
    case "className":
        node.className = value;
        break;
    case "float":
    case "cssFloat":
    case "styleFloat":
        node.style.cssFloat = value;
        node.style.styleFloat = value;
        break;
    default:
        node.style[name] = value;
    }
}

/**
 * Sets a set of style properties on a DOM node.
 *
 * @param node               the HTML DOM node
 * @param styles             the object with name-value pairs
 */
CssUtil.setStyles = function(node, styles) {
    if (styles != null) {
        for (var name in styles) {
            CssUtil.setStyle(node, name, styles[name]);
        }
    }
}
