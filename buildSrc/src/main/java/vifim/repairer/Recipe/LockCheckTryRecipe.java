package vifim.repairer.Recipe;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Try;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LockCheckTryRecipe extends Recipe {

    static int count = 0;

    @Override
    public String getDisplayName() {
        return "Check Lock() try{}";
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        // getVisitor() should always return a new instance of the visitor to avoid any state leaking between cycles
        return new LockCheckTryVisitor();
    }

    public class LockCheckTryVisitor extends JavaIsoVisitor<ExecutionContext> {
        private JavaTemplate unLockTemplate = template("#{}.unlock();").build();

        private JavaTemplate notNullCheckTemplate = template(
                "if (#{} == null) {" +
                        "   throw new NullPointerException(\"#{}\");" +
                        "}"
        )
                .build();

        public Try visitTry(J.Try tryFinally, ExecutionContext c) {
            Try t = super.visitTry(tryFinally, c);

            // Check if the Try contains lock
            boolean lockMethodExists = t.getBody().getStatements().stream()
                    .filter(statement -> statement instanceof J.MethodInvocation)
                    .map(J.MethodInvocation.class::cast)
                    .anyMatch(methodInvocation -> methodInvocation.getName().getSimpleName().equals("lock"));
            if (!lockMethodExists) {
                return t;
            }

            //collect all the "Try"
            List<J.Try> tryList = t.getBody().getStatements().stream()
                    .filter(statement -> statement instanceof J.Try)
                    .map(J.Try.class::cast)
                    .collect(Collectors.toList());
            //set try situation detect
            boolean isTry = false;
            J.Try try1 = null;
            if (tryList.size() == 1) {
                isTry = true;
                try1 = tryList.get(0);
            }

            //collect all the lock methodInvocations
            List<J.MethodInvocation> lockList = t.getBody().getStatements().stream()
                    .filter(statement -> statement instanceof J.MethodInvocation)
                    .map(J.MethodInvocation.class::cast)
                    .filter(methodInvocation -> methodInvocation.getName().getSimpleName().equals("lock"))
                    .collect(Collectors.toList());

            //collect all the unlock methodInvocations
            List<J.MethodInvocation> unlockList = t.getBody().getStatements().stream()
                    .filter(statement -> statement instanceof J.MethodInvocation)
                    .map(J.MethodInvocation.class::cast)
                    .filter(methodInvocation -> methodInvocation.getName().getSimpleName().equals("unlock"))
                    .collect(Collectors.toList());
            if (isTry) {
                unlockList.addAll(
                        try1.getFinally().getStatements().stream()
                                .filter(statement -> statement instanceof J.MethodInvocation)
                                .map(J.MethodInvocation.class::cast)
                                .filter(methodInvocation -> methodInvocation.getName().getSimpleName().equals("unlock"))
                                .collect(Collectors.toList())
                );
            }

            //check if each lock has its own unlcok
            Map<String, Integer> unlockMap = unlockList.stream()
                    .collect(Collectors.toMap(mi -> mi.getSelect().print(),
                            mi -> {
                                return (new Integer(1));
                            },
                            (oldI, newI) -> {
                                return oldI = oldI + 1;
                            }));
            boolean lockUnmatch = false;
            int missTimes = 0;

            Map<String, Integer> map = new HashMap<>();
            for (J.MethodInvocation locks : lockList) {
                boolean typeExist = false;
                try {
                    if (locks.getSelect().getType().toString().equals("Class{java.util.concurrent.locks.ReentrantLock}")) {
                        typeExist = true;
                    }
                } catch (Exception e) {
                    ;
                }
                if (typeExist) {
                    if (map.get(locks.getSelect().print()) == null) {
                        map.put(locks.getSelect().print(), new Integer(1));
                    } else {
                        map.put(locks.getSelect().print(), map.get(locks.getSelect().print()) + 1);
                    }
                }
            }
            for (J.MethodInvocation locks : lockList) {
                boolean typeExist = false;
                try {
                    locks.getSelect().getType().toString();
                    if (locks.getSelect().getType().toString().equals("Class{java.util.concurrent.locks.ReentrantLock}")) {
                        typeExist = true;
                    }
                } catch (Exception e) {
                    ;
                }

                if (typeExist == false) {
                    missTimes++;
                }

                if (typeExist) {
                    if (unlockMap.get(locks.getSelect().print()) == null || unlockMap.get(locks.getSelect().print()) < map.get(locks.getSelect().print())) {
                        if (isTry) {
                            t = t.withTemplate(
                                    unLockTemplate,
                                    try1.getFinally().getCoordinates().lastStatement(),
                                    locks.getSelect().print()
                            );
                        }
                        if (unlockMap.get(locks.getSelect().print()) == null) {
                            unlockMap.put(locks.getSelect().print(), (new Integer(1)));
                        } else {
                            unlockMap.put(locks.getSelect().print(), unlockMap.get(locks.getSelect().print()) + 1);
                        }
                    }
                }
                lockUnmatch = true;
            }

            if (missTimes == lockList.size()) {
                if (count < 1) {
                    AddImport<ExecutionContext> op = new AddImport<ExecutionContext>("java.util.concurrent.locks.ReentrantLock", null, false);
                    // doAfterVisit(op);
                    if (!getAfterVisit().contains(op)) {
                        ++count;
                        doAfterVisit(op);
                    }
                }
            }
            return t;
        }
    }
}
