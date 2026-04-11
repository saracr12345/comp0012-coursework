package comp0012.target;

public class ExtendedOptimisation {

    // ---------------- Task 1: Simple Folding ----------------

    public int simpleInt() {
        return (67 + 12345) - (100 - 25);
    }

    public long simpleLong() {
        return 10000000000L + 2L;
    }

    public double simpleDouble() {
        return 1.5 + 2.5;
    }

    public float simpleFloat() {
        return 2.5f + 1.25f;
    }

    public int simpleIntDivByZero() {
        int x = 5;
        try {
            return x / 0;
        } catch (ArithmeticException e) {
            return 99;
        }
    }

    // ---------------- Task 2: Constant Variables ----------------

    public int constantVariableInt() {
        int a = 62;
        int b = (a + 764) * 3;
        return b + 1234 - a;
    }

    public double constantVariableDouble() {
        double x = 0.67;
        return x * 2.0;
    }

    // ---------------- Task 3: Dynamic Variables ----------------

    public int dynamicVariableReassignment() {
        int a = 42;
        int b = (a + 764) * 3;
        a = b - 67;
        return b + 1234 - a;
    }

    public int dynamicVariableIinc() {
        int x = 0;
        x++;
        int y = x + 3;
        y++;
        return x + y;
    }

    public int dynamicMultipleReassignments() {
        int a = 5;
        int b = a + 2;   // 7
        a = 10;
        int c = a + 3;   // 13
        return b + c;    // 20
    }

    public int dynamicNonConstantKill(int z) {
        int a = 5;
        int b = a + 2;   // can use a = 5 here
        a = z;           // a is no longer known constant after this
        return b;
    }

    public int dynamicNegativeIinc() {
        int x = 10;
        x--;
        return x + 2;
    }

    // ---------------- Task 4: Additional Peephole Optimisation ----------------
    // These are designed so Tasks 2/3 expose constant comparisons,
    // and Task 4 can fold the branches.

    public int constantBranchNotTaken() {
        int x = 12345;
        int y = 54321;

        if (x >= y) {
            return 1;
        }

        if (x <= 0) {
            return 2;
        }

        return 3;
    }

    public int constantBranchTaken() {
        int x = 12345;
        int y = 54321;

        if (x <= y) {
            return 7;
        }

        return 9;
    }

    public int constantUnaryBranchTaken() {
        int x = 12345;
        if (x > 0) {
            return 8;
        }
        return 9;
    }
}