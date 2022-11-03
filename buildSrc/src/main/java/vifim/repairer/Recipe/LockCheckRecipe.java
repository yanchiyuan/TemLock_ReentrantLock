/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package vifim.repairer.Recipe;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.MethodDeclaration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LockCheckRecipe extends Recipe {
    // Making your recipe immutable helps make them idempotent and eliminates categories of possible bugs
    // Configuring your recipe in this way also guarantees that basic validation of parameters will be done for you by rewrite
    // @Option(displayName = "Fully Qualified Class Name",
    //         description = "A fully-qualified class name indicating which class to detect lock() method.",
    //         example = "com.yourorg.FooBar")
    // @NonNull
    // private final String fullyQualifiedMethodName;

    // Recipes must be serializable. This is verified by RecipeTest.assertChanged() and RecipeTest.assertUnchanged()
    // @JsonCreator
    // public LockCheckRecipe(@NonNull @JsonProperty("fullyQualifiedMethodName") String fullyQualifiedMethodName) {
    //     this.fullyQualifiedMethodName = fullyQualifiedMethodName;
    // }
    static long startTime=System.currentTimeMillis();//get start time
    static long totalRepairTime=0;//get start time
    static int count=0;

    @Override
    public String getDisplayName() {
        return "Check Lock()";
    }

    @Override
    public String getDescription() {
        return "check if each lock() has its own unlock() and repair it";
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        // getVisitor() should always return a new instance of the visitor to avoid any state leaking between cycles
        return new vifim.repairer.Recipe.LockCheckRecipe.LockCheckVisitor();
    }

    public class LockCheckVisitor extends JavaIsoVisitor<ExecutionContext> {
        //This visitor uses a method matcher, and it's point-cut syntax, to target the method declaration that will be refactored
        // private MethodMatcher methodMatcher = new MethodMatcher("repairer.LockTest testLock()");

        private  JavaTemplate unLockTemplate = template("#{}.unlock();").build();

        private  JavaTemplate notNullCheckTemplate = template(
                "if (#{} == null) {"+
                        "   throw new NullPointerException(\"#{}\");"+
                        "}"
        )
                .build();
        // private final JavaTemplate testTemplate = template("#{}.#{}.unlock();").build();//test

        @Override
        public MethodDeclaration visitMethodDeclaration(MethodDeclaration method, ExecutionContext c) {
            J.MethodDeclaration m = super.visitMethodDeclaration(method, c);

            // Check if the method contains lock
            boolean lockMethodExists = m.getBody().getStatements().stream()
                    .filter(statement -> statement instanceof J.MethodInvocation)
                    .map(J.MethodInvocation.class::cast)
                    .anyMatch(methodInvocation -> methodInvocation.getName().getSimpleName().equals("lock"));
            if (!lockMethodExists) {
                return m;
            }

            //collect all the "Try"
            List<J.Try> tryList = m.getBody().getStatements().stream()
                    .filter(statement -> statement instanceof J.Try)
                    .map(J.Try.class::cast)
                    .collect(Collectors.toList());
            //set try situation detect
            boolean isTry=false;
            J.Try try1 = null;
            if(tryList.size()==1){
                isTry=true;
                try1 = tryList.get(0);
            }

//            //collect add the "If"
//            List<J.If> IfList = m.getBody().getStatement().stream()
//            .filter(statement -> statement instanceof J.If)
//            .map(J.If.class::cast)
//            .collect(Collectors.toList());
//            //set if situation detect
//            boolean isIf = false;
//            J.If if1 = null;
//            if(ifList.size()==1){
//                isIf=true;
//                if1 = ifList.get(0);
//            }

            //collect all the lock methodInvocations
            List<J.MethodInvocation> lockList = m.getBody().getStatements().stream()
                    .filter(statement -> statement instanceof J.MethodInvocation)
                    .map(J.MethodInvocation.class::cast)
                    .filter(methodInvocation -> methodInvocation.getName().getSimpleName().equals("lock"))
                    .collect(Collectors.toList());

            //collect all the unlock methodInvocations
            List<J.MethodInvocation> unlockList = m.getBody().getStatements().stream()
                    .filter(statement -> statement instanceof J.MethodInvocation)
                    .map(J.MethodInvocation.class::cast)
                    .filter(methodInvocation -> methodInvocation.getName().getSimpleName().equals("unlock"))
                    .collect(Collectors.toList());
            if(isTry){
                unlockList.addAll(
                        try1.getFinally().getStatements().stream()
                                .filter(statement -> statement instanceof J.MethodInvocation)
                                .map(J.MethodInvocation.class::cast)
                                .filter(methodInvocation -> methodInvocation.getName().getSimpleName().equals("unlock"))
                                .collect(Collectors.toList())
                );
            }

            //check if each lock has its own unlcok
            Map<String,Integer> unlockMap = unlockList.stream()
                    .collect(Collectors.toMap(mi -> mi.getSelect().print() ,
                            mi ->{
                                return (new Integer(1));
                            } ,
                            (oldI, newI) -> {
                                return oldI=oldI+1;
                            }));
            boolean lockUnmatch = false;
            int missTimes=0;

            Map<String,Integer> map = new HashMap<>();
            for(J.MethodInvocation locks : lockList ){
                boolean typeExist = false;
                //System.out.print("suo1"+":"+locks.getSelect().getType().toString());
                try{
                    if(locks.getSelect().getType().toString().equals("Class{java.util.concurrent.locks.ReentrantLock}")){
                        typeExist= true;
                    }
                }catch(Exception e){
                    ;
                }
                if(typeExist){
                    if(map.get(locks.getSelect().print())==null){
                        map.put(locks.getSelect().print(),new Integer(1));
                    }else{
                        map.put(locks.getSelect().print(),map.get(locks.getSelect().print())+1);
                    }
                }
            }
            for(J.MethodInvocation locks : lockList ){
                boolean typeExist = false;
                try{
                    locks.getSelect().getType().toString();
                    if(locks.getSelect().getType().toString().equals("Class{java.util.concurrent.locks.ReentrantLock}")){
                        typeExist= true;
                    }
                }catch(Exception e){;}

                if(typeExist==false){
                    missTimes++;
                }

                if(typeExist){
                    long repairTime=System.currentTimeMillis();//get repair time
                    if(unlockMap.get(locks.getSelect().print())==null||unlockMap.get(locks.getSelect().print())<map.get(locks.getSelect().print())){
                        if(isTry){
                            m = m.withTemplate(
                                    unLockTemplate,
                                    try1.getFinally().getCoordinates().lastStatement(),
                                    locks.getSelect().print()
                            );
                        }else{
                            m = m.withTemplate(
                                    unLockTemplate,
                                    m.getBody().getCoordinates().lastStatement(),
                                    locks.getSelect().print()
                            );
                        }
                        if(unlockMap.get(locks.getSelect().print())==null){
                            unlockMap.put(locks.getSelect().print(),(new Integer(1)));
                        }else{
                            unlockMap.put(locks.getSelect().print(),unlockMap.get(locks.getSelect().print())+1);
                        }
                    }
                    long repairEndTime=System.currentTimeMillis();//get repair time
                    totalRepairTime += repairEndTime-repairTime;//add repair time
                }
                lockUnmatch = true;
            }

            if(missTimes==lockList.size()){
                if(count<1){
                    AddImport<ExecutionContext> op = new AddImport<ExecutionContext>("java.util.concurrent.locks.ReentrantLock", null, false);
                    // doAfterVisit(op);
                    if (!getAfterVisit().contains(op)) {
                        ++count;
                        doAfterVisit(op);
                    }
                }
            }
            long endTime=System.currentTimeMillis();//get end time
            Long totalTime = endTime-startTime;
            System.out.println("start time: "+(startTime)+"ms");//total run time
            System.out.println("running time: "+(totalTime)+"ms");//total run time
            System.out.println("detect time: "+(totalTime-totalRepairTime)+"ms");//total detect time
            System.out.println("repair time: "+(totalRepairTime)+"ms");//total repair time
            return m;
        }
    }
}