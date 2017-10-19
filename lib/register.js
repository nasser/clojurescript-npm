
const fs = require('fs');
const cljs = require('clojurescript');

require.extensions['.cljs'] = (module, filename) => {
  const src = fs.readFileSync(filename, 'utf8');
  const ctx = cljs.defaultContext; // cljs.newContext();
  cljs.eval(src, ctx);
  module.exports = cljs.eval('*ns*', ctx).obj;
}
