import js from "@eslint/js";
import globals from "globals";
import stylistic from '@stylistic/eslint-plugin'
import html from "eslint-plugin-html";
import yml from "eslint-plugin-yml";

export default [
    js.configs.recommended,
    ...yml.configs['flat/recommended'],
    {
        languageOptions: {
            ecmaVersion: 2020,
            globals: {
                ...globals.browser,
                RapidContext: "readonly",
            },
        },
        plugins: {
            '@stylistic': stylistic
        },
        rules: {
            "array-callback-return": "error",
            "camelcase": ["error", { "properties": "never" }],
            "complexity": ["error", 25],
            "consistent-return": "error",
            "curly": ["error", "all"],
            "default-case-last": "error",
            "max-depth": ["error", 4],
            "max-params": ["error", 7],
            "new-cap": ["error", { "capIsNew": false }],
            "no-alert": "error",
            "no-array-constructor": "error",
            "no-bitwise": "error",
            "no-caller": "error",
            "no-eval": "error",
            "no-extend-native": "error",
            "no-extra-bind": "error",
            "no-extra-label": "error",
            "no-implied-eval": "error",
            "no-inner-declarations": "error",
            "no-labels": "error",
            "no-loop-func": "error",
            "no-new": "error",
            "no-new-func": "error",
            "no-new-object": "error",
            "no-new-wrappers": "error",
            "no-octal-escape": "error",
            "no-promise-executor-return": "error",
            "no-prototype-builtins": "error",
            "no-redeclare": ["error", { "builtinGlobals": false }],
            "no-self-compare": "error",
            "no-template-curly-in-string": "error",
            "no-throw-literal": "error",
            "no-unmodified-loop-condition": "error",
            "no-unreachable-loop": "error",
            "no-unused-vars": ["error", { "args": "none", "caughtErrors": "none" }],
            "no-useless-assignment": "error",
            "no-useless-return": "error",
            "no-var": "error",
            "no-with": "error",
            "prefer-const": "error",
            //"prefer-object-has-own": "error",
            "prefer-object-spread": "error",
            "prefer-promise-reject-errors": "error",
            "prefer-regex-literals": "error",
            "prefer-spread": "error",
            "prefer-template": "error",
            "radix": "error",
            "require-atomic-updates": "error",
            "require-await": "error",
            "wrap-iife": ["error", "inside"],
            "@stylistic/array-bracket-spacing": ["error", "never"],
            "@stylistic/brace-style": ["error", "1tbs"],
            "@stylistic/comma-spacing": "error",
            "@stylistic/comma-style": "error",
            "@stylistic/eol-last": "error",
            "@stylistic/func-call-spacing": "error",
            "@stylistic/indent": ["error", 4, { "SwitchCase": 0 }],
            "@stylistic/key-spacing": "error",
            "@stylistic/keyword-spacing": "error",
            "@stylistic/linebreak-style": ["error", "unix"],
            "@stylistic/max-len": ["error", { "code": 120 }],
            "@stylistic/no-multi-spaces": "error",
            "@stylistic/no-tabs": "error",
            "@stylistic/no-trailing-spaces": "error",
            "@stylistic/object-curly-newline": ["error", { "consistent": true }],
            "@stylistic/object-curly-spacing": ["error", "always"],
            "@stylistic/operator-linebreak": "error",
            "@stylistic/quotes": ["error", "double"],
            "@stylistic/space-before-blocks": "error",
            "@stylistic/space-before-function-paren": ["error", { "named": "never" }],
            "@stylistic/space-infix-ops": "error",
            "@stylistic/semi": ["error", "always"],
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
            "@stylistic/quotes": ["error", "single"]
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
