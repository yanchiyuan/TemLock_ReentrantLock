package vifim.test

import org.junit.jupiter.api.Test
import org.openrewrite.RecipeTest
import org.openrewrite.java.JavaParser

import vifim.repairer.Recipe.*

class SayHelloRecipeTest(): RecipeTest {
    override val parser = JavaParser.fromJavaVersion().build()
    override val recipe = SayHelloRecipe("repairer.Changed")

    @Test
    fun addsHelloToA() = assertChanged(
        before = """
            package repairer;
            import java.util.concurrent.locks.ReentrantLock;

            public class Changed {
                private ReentrantLock lock = new ReentrantLock();

                public void concurrent(){
                    lock.lock();
                }
            }
        """,
        after = """
            package repairer;
            import java.util.concurrent.locks.ReentrantLock;

            public class Changed {
                private ReentrantLock lock = new ReentrantLock();

                public void concurrent(){
                    lock.lock();
                }

                public String hello() {
                    System.out.println("Generate by repairer!");
                    return "Hello from repairer.Changed!";
                }
            }
        """
    )

    @Test
    fun doesNotChangeExistingHello() = assertUnchanged(
        before = """
            package com.yourorg;

            class A {
                public String hello() { return ""; }
            }
        """
    )

    @Test
    fun doesNotChangeOtherClass() = assertUnchanged(
        before = """
            package com.yourorg;

            class B {
            }
        """
    )
}