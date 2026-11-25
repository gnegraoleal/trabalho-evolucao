// Rodar com Node.js e TypeScript instalado globalmente ou no frontend.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
let ts;
try { ts = require('../trabalho-evolucao-frontend/node_modules/typescript'); }
catch { ts = require('typescript'); }
const dir = path.join(__dirname,'../trabalho-evolucao-frontend/src');
const policy = fs.readFileSync(path.join(dir,'app/auth.models.ts'),'utf8');
const js = ts.transpileModule(policy,{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.CommonJS}}).outputText;
const sandbox={exports:{}};vm.runInNewContext(js,sandbox);const m=sandbox.exports;
let n=0;for(const name of ['abc','ABC_123','A'.repeat(16)]){assert(m.NOME_PATTERN.test(name));n++;}
for(const name of ['ab','A'.repeat(17),'abc-1','ábc',' abc']){assert(!m.NOME_PATTERN.test(name));n++;}
for(const size of [6,128]){assert(m.senhaValida('🔐'.repeat(size)));n++;}
for(const size of [5,129]){assert(!m.senhaValida('🔐'.repeat(size)));n++;}
assert.equal(m.validarNovaSenha('',''),'');n++;assert(m.validarNovaSenha('','',true));n++;assert(m.validarNovaSenha('abcdef','abcdefg'));n++;
console.log(`TypeScript: ${n} verificações independentes da política de conta passaram.`);
let total=0,errors=0;
function walk(p){for(const f of fs.readdirSync(p)){const a=path.join(p,f);if(fs.statSync(a).isDirectory())walk(a);else if(a.endsWith('.ts')){total++;const d=ts.transpileModule(fs.readFileSync(a,'utf8'),{fileName:a,compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.ESNext,experimentalDecorators:true,isolatedModules:true},reportDiagnostics:true}).diagnostics||[];for(const x of d.filter(x=>x.category===ts.DiagnosticCategory.Error)){errors++;console.log(a,ts.flattenDiagnosticMessageText(x.messageText,'\n'));}}}}
walk(dir);console.log(`TypeScript: ${total} arquivos, ${errors} erros sintáticos (transpilação isolada).`);if(errors)process.exitCode=1;
