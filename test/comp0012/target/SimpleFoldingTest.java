package comp0012.target;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;

import static org.junit.Assert.assertEquals;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

//test simple folding
public class SimpleFoldingTest {

    SimpleFolding sf = new SimpleFolding();

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Before
    public void setUpStreams() {
        outContent.reset();
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void cleanUpStreams() {
        System.setOut(originalOut);
    }

    @Test
    public void testSimple() {
        sf.simple();
        assertEquals("12412\n", outContent.toString());
    }

    @Test
    public void testSimpleSub() {
        sf.simpleSub();
        assertEquals("75\n", outContent.toString());
    }

    @Test
    public void testSimpleMul() {
        sf.simpleMul();
        assertEquals("42\n", outContent.toString());
    }

    @Test
    public void testSimpleDiv() {
        sf.simpleDiv();
        assertEquals("4\n", outContent.toString());
    }

    @Test
    public void testSimpleLongAdd() {
        sf.simpleLongAdd();
        assertEquals("10000000002\n", outContent.toString());
    }

    @Test
    public void testSimpleDoubleAdd() {
        sf.simpleDoubleAdd();
        assertEquals("4.0\n", outContent.toString());
    }
}
