package vifim.repairer.Recipe;

import java.util.stream.Collectors;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.MethodDeclaration;
import org.openrewrite.java.tree.J.Modifier.Type;
import org.openrewrite.internal.ListUtils;

public class ExpandCustomerInfo extends Recipe {
    //try stop triple refraction
    public static int flag = 0;
    //Rewrite provides a managed environment in which it discovers, instantiates, and wires configuration into Recipes.
    //This recipe has no configuration and when it is executed, it will delegate to its visitor.
    @Override
    public String getDisplayName() {
        return "Expand Customer Info";
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        // getVisitor() should always return a new instance of the visitor to avoid any state leaking between cycles
        return new ExpandCustomerInfoVisitor();
    }

    // @Override
    // protected TreeVisitor<?, ExecutionContext> getVisitor() {
    //     return new ExpandCustomerInfoVisitor();
    // }

    private class ExpandCustomerInfoVisitor extends JavaIsoVisitor<ExecutionContext> {

        //This visitor uses a method matcher, and it's point-cut syntax, to target the method declaration that will be refactored
        private MethodMatcher methodMatcher = new MethodMatcher("repairer.Customer setCustomerInfo(String)");

        //Template used to add a method body to "setCustomerInfo()" method declaration.
        private JavaTemplate addMethodBodyTemplate = template("{this.lastName = lastName;}")
                .build();

        //Template used to insert two additional parameters into the "setCustomerInfo()" method declaration.
        private JavaTemplate addMethodParametersTemplate = template("Date dateOfBirth, String firstName,")
                .imports("java.util.Date")
                .build();

        //Template used to add two initializing statements to the method body
        private JavaTemplate addStatementsTemplate = template("this.dateOfBirth=dateOfBirth;\nthis.firstName = firstName;")
                .imports("java.util.Date")
                .build();
        @Override
        public MethodDeclaration visitMethodDeclaration(MethodDeclaration method, ExecutionContext c) {
            J.MethodDeclaration m = super.visitMethodDeclaration(method, c);
            System.out.println("method.getType():"+method.getType());

            if (!methodMatcher.matches(method.getType())) {
                return m;
            }

                //Remove the abstract modifier from the method.
                // m = m.withModifiers(m.getModifiers().stream().filter(mod -> mod.getType() != Type.Abstract).collect(Collectors.toList()));
                m = m.withModifiers(ListUtils.map(m.getModifiers(),mod -> mod.getType() == J.Modifier.Type.Abstract ? null : mod));
                //Add a method body use the JavaTemplate by using the "replaceBody" coordinates.
                // m = m.withTemplate(addMethodBodyTemplate, m.getCoordinates().replaceBody());


                //Add two parameters to the method declaration by inserting them in from of the first argument.
                m = m.withTemplate(addMethodParametersTemplate, m.getParameters().get(0).getCoordinates().before());
                //Add two additional statements to method's body by inserting them in front of the first statement
                // m = m.withTemplate(addStatementsTemplate, m.getBody().getStatements().get(0).getCoordinates().before());
                //Need to make sure that the Date type is added to this compilation unit's list of imports.
                maybeAddImport("java.util.Date");
                return m;

        }
    }
}