
const fs = require('fs')
const path = require('path')
const lib = path.join(path.dirname(fs.realpathSync(__filename)), '../lib')
const cljs = require(`${lib}/clojurescript`)
const readline = require('readline')

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
})

const prompt = (p) => {
  const promptString = p || `${cljs.eval('(str *ns*)')}=> `
  rl.setPrompt(promptString)
  rl.prompt()
}

const repl = () => {
  process.stdout.write('cljs.user=> ')

  // http://codereview.stackexchange.com/questions/45991/balanced-parentheses
  const parenthesesAreBalanced = (s) => {
    const parentheses = '[]{}()'

    //Parentheses stack
    const stack = []

     //Character in the string
    let character = ''

    for (let i = 0; character = s[i++];) {
      const bracePosition = parentheses.indexOf(character)
      let braceType = ''

      //~ is truthy for any number but -1
      if (!~bracePosition) {
        continue
      }

      braceType = bracePosition % 2 ? 'closed' : 'open'

      if (braceType === 'closed') {
        //If there is no open parenthese at all, return false OR
        //if the opening parenthese does not match ( they should be neighbours )
        if (!stack.length || parentheses.indexOf(stack.pop()) != bracePosition - 1)
          return false
      } else {
        stack.push(character)
      }
    }

    //If anything is left on the stack <- not balanced
    return !stack.length
  }

  let input = ''

  rl.on('line', (cmd) => {
    input = `${input}${cmd}\n`
    if(cmd.length > 0 && parenthesesAreBalanced(input)) {
      try {
        const x = cljs.eval(input)
        input = ''
        cljs.context.cljs.pprint.pprint(x)
      } catch(e) {
        console.error(e)
      }
      prompt()

    } else {
      prompt('... ')
    }
  })

  rl.on('close', () => {
    process.exit(0)
  })

  prompt()
}

module.exports = { startREPL: repl }
