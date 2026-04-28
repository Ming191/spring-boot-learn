public static class A {
    private final IDependency dependency;
    A(IDependency dependency) {
        this.dependency = dependency;
    }

    public void doSmt() {
        dependency.check();
    }
}

public interface IDependency {
    void check();
}

public static class DependencyImpl implements IDependency {
    @Override
    public void check() {
        IO.println("DependencyImpl");
    }
}

void main() {
    IDependency dep = new DependencyImpl();
    A a = new A(dep);
    a.doSmt();
}
