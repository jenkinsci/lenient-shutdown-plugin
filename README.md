lenient-shutdown-plugin
=======================

This plugin lets you put Jenkins in shutdown mode but still allow any downstream builds of those currently running to also complete.
Similar functionality for taking nodes temporarily offline.

## Building

### One-time set-up: using mise-en-place

If you have the [mise-en-place](https://mise.jdx.dev/) tool manager installed, you can perform these one-time steps:
1. Trust this repository via `mise trust`
2. Install the tools via `mise install`
3. Confirm it's working via `mvn --version`

### One-time set-up: manually

1. Install the version of OpenJDK called out by the `.java-version` file.
2. Install the version of Maven called out by the `maven` key in the `mise.toml` file.
3. Confirm it's working via `mvn --version`

### Compile and test

Most of the time, you'll want to invoke:

```bash
mvn clean test
```

This should compile the code and the test code, then run the tests.


### Other interesting targets

Aside from `clean` and `test` demonstrated above, here are other targets you can give Maven:

| Target            | Description                                                                      |
|-------------------|----------------------------------------------------------------------------------|
| `javadoc:javadoc` | Parses the JavaDoc comments and generates the `target/site/apidocs` directory    |
| `hpi:hpi`         | Creates the `target/lenientshutdown.hpi` file, for testing the plugin in Jenkins |


License
=======

    The MIT License
    
    Copyright (c) 2014 Sony Mobile Communications Inc. All rights reserved.
    
    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:
    
    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.
    
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
