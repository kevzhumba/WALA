package cpa;

public class ReceiverDispatch {

  static class A {
    Object foo() {
      return this;
    }
  }

  static class B extends A {
    @Override
    Object foo() {
      return this;
    }
  }

  public static void main(String[] args) {
    A x = args.length > 0 ? new A() : new B();
    x.foo();
  }
}
