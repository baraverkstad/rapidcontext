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

import fn from './fn.mjs';
import ui from './ui/index.mjs';

export default { fn, ui };

// Note: Setting window.RapidContext here for legacy global API
let RapidContext = window.RapidContext || (window.RapidContext = {});
Object.assign(RapidContext, { Fn: fn, UI: RapidContext.UI || {} });
Object.assign(RapidContext.UI, { Event: ui.event, Msg: ui.msg });
