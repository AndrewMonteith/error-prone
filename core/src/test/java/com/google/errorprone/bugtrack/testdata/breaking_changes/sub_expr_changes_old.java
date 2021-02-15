class Foo {
    public boolean greaterThanFive(int x) {
        return 5 < x;
    }

    public int addTwo(int x) {
        return x + 2;
    }

//    public String arrayToStringShort(String[] s) {
//        return s.toString();
//    }

//    public String arrayToStringDoesntWork(String[] s) {
//        return s.toString();
//    }

    private void someMethod(String[] s) {}

    public String bigMethod(String[] s) {
        someMethod(s);
        someMethod(s);
        someMethod(s);

        String str = s.toString().strip();

        someMethod(s);
        someMethod(s);
        someMethod(s);

        return "";
    }
}