import js from "@eslint/js";
import globals from "globals";
import html from "eslint-plugin-html";

export default [
    js.configs.recommended,
    {
        languageOptions: {
            ecmaVersion: 2020,
            globals: {
                ...globals.browser,
                RapidContext: "readonly",
            },
        },
        rules: {
            "array-bracket-spacing": ["error", "never"],
            "brace-style": ["error", "1tbs"],
            "camelcase": ["error", { "properties": "never" }],
            "comma-spacing": "error",
            "comma-style": "error",
            "complexity": ["error", 25],
            "consistent-return": "error",
            "curly": ["error", "all"],
            "eol-last": "error",
            "func-call-spacing": "error",
            "indent": ["error", 4],
            "key-spacing": "error",
            "keyword-spacing": "error",
            "linebreak-style": ["error", "unix"],
            "max-depth": ["error", 4],
            "max-len": ["error", { "code": 120 }],
            "max-params": ["error", 7],
            "new-cap": ["error", { "capIsNew": false }],
            "no-array-constructor": "error",
            "no-bitwise": "error",
            "no-caller": "error",
            "no-extend-native": "error",
            "no-multi-spaces": "error",
            "no-new": "error",
            "no-new-func": "error",
            "no-new-object": "error",
            "no-new-wrappers": "error",
            "no-octal-escape": "error",
            "no-prototype-builtins": "error",
            "no-redeclare": ["error", { "builtinGlobals": false }],
            "no-throw-literal": "error",
            "no-trailing-spaces": "error",
            "no-unused-vars": ["error", { "args": "none", "caughtErrors": "none" }],
            "no-useless-return": "error",
            "no-with": "error",
            "object-curly-newline": ["error", { "consistent": true }],
            "object-curly-spacing": ["error", "always"],
            "operator-linebreak": "error",
            "prefer-promise-reject-errors": "error",
            "quotes": ["error", "double"],
            "semi": ["error", "always"],
            "space-before-blocks": "error",
            "space-before-function-paren": ["error", { "named": "never" }],
            "space-infix-ops": "error",
            "wrap-iife": ["error", "inside"]
        },
    },
    {
        files: ["src/plugin/**/*.{js,cjs}", "test/src/js/**/*.{js,cjs}"],
        languageOptions: {
            ecmaVersion: 2020,
            globals: {
                ...globals.browser,
                $: "readonly",
                CryptoJS: "readonly",
                MochiKit: "readonly",
                RapidContext: "writable",
            },
        },
    },
    {
        files: ["src/plugin/**/*.mjs", "test/src/js/**/*.mjs"],
        languageOptions: {
            sourceType: "module",
        },
        rules: {
            "quotes": ["error", "single"]
        }
    },
    {
        files: ["src/plugin/**/*.{html,tmpl}"],
        plugins: { html },
        settings: {
            "html/html-extensions": [".html", ".tmpl"]
        },
    },
];
