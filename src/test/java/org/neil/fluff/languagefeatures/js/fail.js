function main() {
  otherFunction();
}

function otherFunction() {
  failingFunction();
}

function failingFunction() {
  throw "failure in JS";
}

main();