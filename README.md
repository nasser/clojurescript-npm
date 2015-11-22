ClojureScript npm Package
=========================

Making the ClojureScript language usable from any Node.js project

```javascript
> var cljs = require("./lib/clojurescript")
undefined
> cljs.compile("(fn [a] (str \"Hello \" a))")
'(function (a){\nreturn [cljs.core.str("Hello "),cljs.core.str(a)].join(\'\');\n})'
> var f = cljs.eval("(fn [a] (str \"Hello \" a))")
undefined
> f("World")
'Hello World'
```

Status
------

Very early. Do not use for anything critical. Contributions welcome!

API
---

The API is modelled after [CoffeeScript](https://github.com/jashkenas/coffeescript)'s

* `cljs.compile(str)` — Takes a string of ClojureScript code and returns a string of JavaScript code
* `cljs.eval(str)` — Takes a string of ClojureScript code, evaluates it, returns the resulting value

Building
--------

After modifying `src`, make sure that `$CLOJURESCRIPT_HOME` is set and run `./scripts/build.sh`. This will replace `lib/core.js`.

Todo
----

* Loading of ClojureScript packages off of maven/clojars

Legal
-----

npm package © [Ramsey Nasser](http://nas.sr/), provided under the [Eclipse Public License 1.0](http://opensource.org/licenses/eclipse-1.0.php)

[ClojureScript](https://github.com/clojure/clojurescript) © Rich Hickey, provided under the [Eclipse Public License 1.0](http://opensource.org/licenses/eclipse-1.0.php)