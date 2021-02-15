class Foo {
    public boolean greaterThanFive(int x) {
        return 5 < x;
    }

    public int addTwo(int x) {
        return x + 2;
    }

//    public String arrayToStringShort(String[] s) {
//        return s.toString().trim();
//    }

//    public String arrayToStringDoesntWork(String[] s) {
//        String str = s.toString();
//        if (!str.isEmpty()) {
//            str = str.strip();
//        }
//        return str;
//    }

    private void someMethod(String[] s) {
    }

    public String bigMethod(String[] s) {
        someMethod(s);
        someMethod(s);
        someMethod(s);

        String str = s.toString();
//        String str2 = s.toString();

        someMethod(s);
        someMethod(s);

        return "";
    }
}
