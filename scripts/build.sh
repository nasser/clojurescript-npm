if [ "$CLOJURESCRIPT_HOME" = "" ]; then
  echo "CLOJURESCRIPT_HOME not set, build will not work!"
  exit 1
fi

SRC="`pwd`/src/clojurescript.cljs"
OUT="`pwd`/lib/core.js"

echo "Compiling ClojureScript, this will take some time..."

$CLOJURESCRIPT_HOME/bin/cljsc $SRC "{:optimizations :simple :target :nodejs}" > $OUT

echo "Done!"