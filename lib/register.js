var fs = require("fs");
var cljs = require("clojurescript");

require.extensions['.cljs'] = function(module, filename) {
  var src = fs.readFileSync(filename, "utf8");
  var ctx = cljs.defaultContext; // cljs.newContext();
  cljs.eval(src, ctx);
  module.exports = cljs.eval("*ns*", ctx).obj;
}