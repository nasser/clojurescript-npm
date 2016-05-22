var fs = require("fs");
var vm = require("vm");

var cljsSrc = fs.readFileSync("lib/bootstrap.js", "utf8");
var cljsScript = new vm.Script(cljsSrc);

function newContext (globals) {
  var sandbox = globals || {require:require,process:process,console:console}
  sandbox.global = sandbox;
  var ctx = vm.createContext(sandbox);
  cljsScript.runInContext(ctx);
  return ctx;
}

var defaultContext = newContext();

function _eval (code, ctx) {
  var context = ctx || defaultContext;
  return context.clojurescript.core.eval(code);
}

function compile (code, ctx) {
  var context = ctx || defaultContext;
  return context.clojurescript.core.compile(code);
}

module.exports = {newContext: newContext,
                  context: defaultContext,
                  compile: compile,
                  eval: _eval};