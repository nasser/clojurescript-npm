ClojureScript npm Package: FORKED EDITION
=========================

# MY CHANGELOG/ADDED FEATURES
- `cljs` binary now has added functionality!
  - `cljs` by itself will launch repl
  - `cljs -e file.cljs` will evalute file and output evaluated code to stdout
  - `cljs -c file.js -o compiled.js` will compile clojurescript code to javascript and save to file
- modularize bin code. introduces `yargs` for cli argument parsing
- updated syntax and node version requirements (y tho? 🐸)


# ORIGINAL DOCS

Making the [ClojureScript language](https://github.com/clojure/clojurescript) usable from Node.js projects

```js
var cljs = require("clojurescript")
cljs.compile("(fn [a] (str \"Hello \" a))")
// '(function (a){\nreturn [cljs.core.str("Hello "),cljs.core.str(a)].join(\'\');\n})'
var f = cljs.eval("(fn [a] (str \"Hello \" a))")
f("World")
// 'Hello World'
```

Status
------

Very early. Do not use for anything critical. Contributions welcome!

Installation
------------

    npm install -g clojurescript


Usage
-----

### Evaluating & Compiling
`eval` will return values that are directly usable from JavaScript. The whole standard library is available for use.

```js
var f = cljs.eval("(fn [& args] (->> args (map inc) (remove odd?) into-array))")
f(1, 2, 3, 4, 5, 6, 7)
// [ 2, 4, 6, 8 ]
```

`compile` will return a string that can be evaluated using JavaScript's `eval`.

```js
cljs.compile("(if (< 1 2 3 4 5) (.log js/console \"Less\"))")
'((((1 < 2)) && (((2 < 3)) && (((3 < 4)) && ((4 < 5)))))?console.log("Less"):null)'
```

Any code that makes use of namespaces of `def`s will need to run *after* `lib/bootsrap.js` has set up the ClojureScript environment. Contexts take care of this.

```js
cljs.compile("(ns your.namespace) (defn f [x] (str \"your \" x))")
'goog.provide(\'your.namespace\');\ngoog.require(\'cljs.core\');\n(function (){\nyour.namespace.f = (function your$namespace$f(x){\nreturn [cljs.core.str("your "),cljs.core.str(x)].join(\'\');\n}); return (\nnew cljs.core.Var(function(){return your.namespace.f;},new cljs.core.Symbol("your.namespace","f","your.namespace/f",1343508863,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"your.namespace","your.namespace",-1885076140,null),new cljs.core.Symbol(null,"f","f",43394975,null),null,28,21,1,1,cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"x","x",-555367584,null)], null)),null,(cljs.core.truth_(your.namespace.f)?your.namespace.f.cljs$lang$test:null)])));})()\n'
```

### Contexts
ClojureScript has its own notion of [namespaces](http://clojure.org/reference/namespaces) implemented by [the Google Closure JavaScript library](https://developers.google.com/closure/library/docs/tutorial#creating-a-namespace-with-googprovide). It sets up nested objects in the global environment to store your functions and data. This works well in the browser, but makes it difficult to work with Node which perfers to leave the global environment untouched.

To reconcile these two approaches, this package only ever evaluates ClojureScript code in an *execution context*. A context is an object that contains the global environment with the Google Closure-style namespaces that ClojureScript expects.

```js
var ctx = cljs.newContext()
ctx.cljs.core.str
// { [Function]
//   ... }
ctx.cljs.core.map
// { [Function]
//   ... }
```

This way, ClojureScript gets the view of the world it wants and the global environment is not affected.

Code can be evaluated in any context. Namespaces and functions are all all accesible and usable from JavaScript.

```js
cljs.eval("(ns foo)", ctx)
cljs.eval("(def name \"Ramsey\")", ctx)
ctx.foo.name
// 'Ramsey'
cljs.eval("(defn surround [a b] (str b a b))", ctx)
ctx.foo.surround("Hello", "~")
// '~Hello~'
```

Seperate contexts do not share anything. Even the standard library is reevaluated from scratch for each new context. Importantly, they maintain their own current namespace in `*ns*`.

```js
cljs.eval("(str *ns*)", ctx)
// 'foo'
var ctx2 = cljs.newContext()
cljs.eval("(str *ns*)", ctx2)
// 'cljs.user'
cljs.eval("(ns foo)", ctx2)
cljs.eval("(def name \"Not Ramsey\")", ctx2)
ctx2.foo.name
// 'Not Ramsey'
```

When `eval` and `compile` are used without a second argument, a default context is used. It is exposed as `context` on the module object.

```js
cljs.eval("(ns foo) (def a 1)")
cljs.context.foo.a
// 1
```

### ClojureScript Require
`:require` and `:use` work in `ns` forms as expected. The search path aims to mirror the Node package resolution algorithm, i.e. `.cljs` files will be looked up in the `node_modules` folder if it exists, and then in the `node_modules` folder of the parent directory and so on to the top of the file system.

```clj
;; node_modules/helpers.cljs
(ns helpers)

(defn wrap [a min max]
  (cond (< a min) min
        (> a max) max
        :else    a))
```
```js
var cljs = require("clojurescript")
cljs.eval("(ns main (:require helpers))")
cljs.eval("(helpers/wrap 90 0 10)")
// 10
cljs.eval("(helpers/wrap -90 0 10)")
// 0
```

### Node Require
`require`ing `"clojurescript/register"` will register the `.cljs` extension with Node's `require` function and allow you to `require` cljs files in node. The module will export the contents of the last namespace in the file.

```js
require("clojurescript/register")
var helpers = require("helpers")
helpers
// { wrap: [Function: helpers$wrap] }
helpers.wrap(90, 0, 100)
// 90
helpers.wrap(-90, 0, 100)
// 0
```


### Command Line

Installing with `-g` will create put a `cljs` executable on your path.

Calling without arguments starts a REPL.

```clj
$ cljs
> (+ 1 2)
3
> (ns mappers)
nil
> (defn inc-all [col] (map inc col))
#'mappers/inc-all
> (ns cljs.user)
nil
> (mappers/inc-all (range 20))
(1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20)
> (ns cljs.user (:require helpers))
nil
> (helpers/wrap 0 0 0)
0
> (ns cljs.user (:require [helpers :refer [wrap]]))
nil
> (wrap 90 0 10)
10
> (wrap -90 0 10)
0
```

Passing a file as an argument will execute that file.
```clj
$ cat foo.cljs
(->> (range 20)
     (map inc)
     (filter even?)
     (map #(println (str "Hello " % " World")))
     dorun)
$ cljs foo.cljs
Hello 2 World
Hello 4 World
Hello 6 World
Hello 8 World
Hello 10 World
Hello 12 World
Hello 14 World
Hello 16 World
Hello 18 World
Hello 20 World
```

API
---

* `cljs.newContext([globals])` Creates a new ClojureScript execution context.
* `cljs.context` The default execution context. Used by `compile` and `eval` by default.
* `cljs.compile(str[, ctx])` — Takes a string of ClojureScript code and returns a string of JavaScript code. Optionally takes a context to compile the code in.
* `cljs.eval(str[, ctx])` — Takes a string of ClojureScript code, evaluates it, and returns the resulting value. Optionally takes a context to evaluate the code in.

Building
--------

    lein cljsbuild once

to rebuild `lib/bootstrap.js`, where the ClojureScript compiler and standard library live. Requires Leiningen, Clojure, and Java to be installed and properly configured.

Legal
-----

npm package © 2015-2016 [Ramsey Nasser](http://nas.sr/), provided under the [Eclipse Public License 1.0](http://opensource.org/licenses/eclipse-1.0.php)

Uses code from [elbow](https://github.com/mfikes/elbow) © 2015–2016 [Mike Fikes](https://github.com/mfikes) and Contributors. Used under the [Eclipse Public License version 1.0](http://opensource.org/licenses/eclipse-1.0.php).

[ClojureScript](https://github.com/clojure/clojurescript) © Rich Hickey, provided under the [Eclipse Public License 1.0](http://opensource.org/licenses/eclipse-1.0.php)