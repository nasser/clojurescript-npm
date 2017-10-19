
const fs = require('fs')
const vm = require('vm')

const cljsSrc = fs.readFileSync(`#{__dirname}/bootstrap.js`, 'utf8')
const cljsScript = new vm.Script(cljsSrc)

const newContext = globals => {
  const sandbox = globals || { module,require,process,console }
  sandbox.global = sandbox
  const ctx = vm.createContext(sandbox)
  cljsScript.runInContext(ctx)
  return ctx
}

const defaultContext = newContext()

const _eval = (code, ctx) => {
  const context = ctx || defaultContext
  return context.clojurescript.core.eval(code)
}

const compile = (code, ctx) => {
  const context = ctx || defaultContext
  return context.clojurescript.core.compile(code)
}

module.exports = {compile,
                  newContext,
                  context: defaultContext,
                  eval: _eval}
