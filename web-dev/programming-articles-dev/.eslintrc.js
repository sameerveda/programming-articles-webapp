module.exports = {
    env: {
        browser: true,
        commonjs: true,
        es6: true,
        node: true,
        jquery: true
    },
    globals: {
        redom: true
    },
    parserOptions: {
        sourceType: "module"
    },
    extends: ["eslint:recommended", "google"],
    rules: {
        indent: [2, 4],
        "linebreak-style": 0,
        semi: [2, "always"],
        "no-console": 0,
        "no-debugger": 0,
        "no-alert": 0,
        eqeqeq: [2, "always"],
        strict: 0,
        "one-var": 0,
        indent: 0,
        "require-jsdoc": 0,
        "arrow-parens": 0,
        "object-curly-spacing": 0,
		"space-in-parens": ["error", "always"],
        "comma-dangle": [
            "error",
            {
                objects: "never"
            }
        ],
        "camelcase": 0,
        "max-len": 0,
        "eol-last": 0,
        "curly": 0,
        "space-before-function-paren": 0,
		"no-undef": "error",
		"brace-style": ["error", "1tbs", { "allowSingleLine": true }]
    }
};