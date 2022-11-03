package vifim.repairer.Recipe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;

public class SayHelloRecipe extends Recipe {
    // Making your recipe immutable helps make them idempotent and eliminates categories of possible bugs
    // Configuring your recipe in this way also guarantees that basic validation of parameters will be done for you by rewrite
    @Option(displayName = "Fully Qualified Class Name",
            description = "A fully-qualified class name indicating which class to add a hello() method.",
            example = "com.yourorg.FooBar")
    @NonNull
    private final String fullyQualifiedClassName;

    // Recipes must be serializable. This is verified by RecipeTest.assertChanged() and RecipeTest.assertUnchanged()
    @JsonCreator
    public SayHelloRecipe(@NonNull @JsonProperty("fullyQualifiedClassName") String fullyQualifiedClassName) {
        this.fullyQualifiedClassName = fullyQualifiedClassName;
    }

    @Override
    public String getDisplayName() {
        return "Say Hello";
    }

    @Override
    public String getDescription() {
        return "Adds a \"hello\" method to the specified class";
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        // getVisitor() should always return a new instance of the visitor to avoid any state leaking between cycles
        return new SayHelloVisitor();
    }

    public class SayHelloVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final JavaTemplate helloTemplate =
                template("public String hello() {\n"
                        +"System.out.println(\"Generate by repairer!\");\n"
                        +"return \"Hello from #{}!\"; }").build();

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            // In any visit() method the call to super() is what causes sub-elements of to be visited
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);

            if (classDecl.getType() == null || !classDecl.getType().getFullyQualifiedName().equals(fullyQualifiedClassName)) {
                // We aren't looking at the specified class so return without making any modifications
                return cd;
            }

            // Check if the class already has a method named "hello" so we don't incorrectly add a second "hello" method
            boolean helloMethodExists = classDecl.getBody().getStatements().stream()
                    .filter(statement -> statement instanceof J.MethodDeclaration)
                    .map(J.MethodDeclaration.class::cast)
                    .anyMatch(methodDeclaration -> methodDeclaration.getName().getSimpleName().equals("hello"));
            if (helloMethodExists) {
                return cd;
            }

            // Interpolate the fullyQualifiedClassName into the template and use the resulting AST to update the class body
            // cd = cd.withBody(
            //         cd.getBody().withTemplate(
            //                 helloTemplate,
            //                 cd.getBody().getCoordinates().lastStatement(),
            //                 fullyQualifiedClassName
            //         ));
            cd =cd.withTemplate(helloTemplate,cd.getBody().getCoordinates().lastStatement(),fullyQualifiedClassName);
            return cd;
        }
    }
}