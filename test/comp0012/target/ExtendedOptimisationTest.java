package comp0012.target;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ExtendedOptimisationTest {

    private final ExtendedOptimisation t = new ExtendedOptimisation();

    // ---------------- Task 1 ----------------

    @Test
    public void testSimpleInt() {
        assertEquals(12337, t.simpleInt());
    }

    @Test
    public void testSimpleLong() {
        assertEquals(10000000002L, t.simpleLong());
    }

    @Test
    public void testSimpleDouble() {
        assertEquals(4.0, t.simpleDouble(), 0.000001);
    }

    @Test
    public void testSimpleFloat() {
        assertEquals(3.75f, t.simpleFloat(), 0.000001f);
    }

    @Test
    public void testSimpleIntDivByZero() {
        assertEquals(99, t.simpleIntDivByZero());
    }

    // ---------------- Task 2 ----------------

    @Test
    public void testConstantVariableInt() {
        assertEquals(3650, t.constantVariableInt());
    }

    @Test
    public void testConstantVariableDouble() {
        assertEquals(1.34, t.constantVariableDouble(), 0.000001);
    }

    // ---------------- Task 3 ----------------

    @Test
    public void testDynamicVariableReassignment() {
        assertEquals(1301, t.dynamicVariableReassignment());
    }

    @Test
    public void testDynamicVariableIinc() {
        assertEquals(6, t.dynamicVariableIinc());
    }

    @Test
    public void testDynamicMultipleReassignments() {
        assertEquals(20, t.dynamicMultipleReassignments());
    }

    @Test
    public void testDynamicNonConstantKill() {
        assertEquals(7, t.dynamicNonConstantKill(123));
    }

    @Test
    public void testDynamicNegativeIinc() {
        assertEquals(11, t.dynamicNegativeIinc());
    }

    // ---------------- Task 4 ----------------

    @Test
    public void testConstantBranchNotTaken() {
        assertEquals(3, t.constantBranchNotTaken());
    }

    @Test
    public void testConstantBranchTaken() {
        assertEquals(7, t.constantBranchTaken());
    }

    @Test
    public void testConstantUnaryBranchTaken() {
        assertEquals(8, t.constantUnaryBranchTaken());
    }
}