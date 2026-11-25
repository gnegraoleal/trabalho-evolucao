import javax.tools.*;
import com.sun.source.util.JavacTask;
import java.nio.file.*;
import java.util.*;
public class ParseJava {
  public static void main(String[] args) throws Exception {
    List<Path> files;
    try (var s=Files.walk(Path.of(args[0]))) {files=s.filter(p->p.toString().endsWith(".java")).toList();}
    var compiler=ToolProvider.getSystemJavaCompiler(); var diagnostics=new DiagnosticCollector<JavaFileObject>();
    try (var fm=compiler.getStandardFileManager(diagnostics,null,null)) {
      var task=(JavacTask)compiler.getTask(null,fm,diagnostics,List.of("-proc:none","--source","21"),null,fm.getJavaFileObjectsFromPaths(files));
      task.parse();
    }
    long errors=diagnostics.getDiagnostics().stream().filter(d->d.getKind()==Diagnostic.Kind.ERROR).peek(System.err::println).count();
    System.out.println("Java: "+files.size()+" arquivos analisados, "+errors+" erros sintáticos.");
    if(errors>0)System.exit(1);
  }
}
