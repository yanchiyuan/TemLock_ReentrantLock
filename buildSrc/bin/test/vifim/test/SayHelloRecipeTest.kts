// package vifim.test

// import org.junit.jupiter.api.Test
// import org.openrewrite.RecipeTest
// import org.openrewrite.java.JavaParser

// class SayHelloRecipeTest(): RecipeTest {
//     override val parser = JavaParser.fromJavaVersion().build()
//     override val recipe = SayHelloRecipe().apply { setFullyQualifiedClassName("com.yourorg.A") }

//     @Test
//     fun addsHelloToA() = assertChanged(
//         before = """
//             package com.yourorg;

//             class A {
//             }
//         """,
//         after = """
//             package com.yourorg;

//             class A {
//                 public String hello() {
//                     return "Hello from com.yourorg.A!";
//                 }
//             }
//         """
//     )

//     @Test
//     fun doesNotChangeExistingHello() = assertUnchanged(
//         before = """
//             package com.yourorg;

//             class A {
//                 public String hello() { return ""; }
//             }
//         """
//     )

//     @Test
//     fun doesNotChangeOtherClass() = assertUnchanged(
//         before = """
//             package com.yourorg;

//             class B {
//             }
//         """
//     )
// }