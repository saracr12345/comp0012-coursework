package comp0012.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.util.InstructionFinder;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.TargetLostException;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LDC2_W;
import org.apache.bcel.generic.ArithmeticInstruction;
//optimisation for the maths 
import org.apache.bcel.generic.PUSH;

import org.apache.bcel.generic.IADD;
import org.apache.bcel.generic.ISUB;
import org.apache.bcel.generic.IMUL;
import org.apache.bcel.generic.IDIV;

import org.apache.bcel.generic.LADD;
import org.apache.bcel.generic.LSUB;
import org.apache.bcel.generic.LMUL;
import org.apache.bcel.generic.LDIV;

import org.apache.bcel.generic.FADD;
import org.apache.bcel.generic.FSUB;
import org.apache.bcel.generic.FMUL;
import org.apache.bcel.generic.FDIV;

import org.apache.bcel.generic.DADD;
import org.apache.bcel.generic.DSUB;
import org.apache.bcel.generic.DMUL;
import org.apache.bcel.generic.DDIV;


public class ConstantFolder
{
	ClassParser parser = null;
	ClassGen gen = null;

	JavaClass original = null;
	JavaClass optimized = null;

	public ConstantFolder(String classFilePath)
	{
		try{
			this.parser = new ClassParser(classFilePath);
			this.original = this.parser.parse();
			this.gen = new ClassGen(this.original);
		} catch(IOException e){
			e.printStackTrace();
		}
	}

	//optimisation code 
	public void optimize()
	{
		ClassGen cgen = new ClassGen(original);
		ConstantPoolGen cpgen = cgen.getConstantPool();

		Method[] methods = cgen.getMethods(); //load all methods in the class

		for (Method method : methods) { //iterate through each method in the class 
			Code code = method.getCode();

			if (code == null) {
				continue; //ignore methods with no code inside
			}

			MethodGen mg = new MethodGen(method, cgen.getClassName(), cpgen); //so we can edit the methods
			InstructionList il = mg.getInstructionList(); //return their code 

			if (il == null) {
				continue;
			} //ignore methods with no code inside

			// Task 1
			boolean changed = true;

			while (changed) {
				changed = false;

				for (InstructionHandle ih = il.getStart(); ih != null; ih = ih.getNext()) {
					InstructionHandle next1 = ih.getNext();
					if (next1 == null) {
						continue;
					}

					InstructionHandle next2 = next1.getNext();
					if (next2 == null) {
						continue;
					}

					Instruction instr1 = ih.getInstruction();
					Instruction instr2 = next1.getInstruction();
					Instruction instr3 = next2.getInstruction();

					Number value1 = getConstantValue(instr1, cpgen);
					Number value2 = getConstantValue(instr2, cpgen);

					if (value1 != null && value2 != null && instr3 instanceof ArithmeticInstruction) {
						Number result = foldOperation(value1, value2, instr3);

						if (result != null) {
							try {
								InstructionList newInstructions = new InstructionList();

								if (result instanceof Integer) {
									newInstructions.append(new PUSH(cpgen, result.intValue()));
								} else if (result instanceof Long) {
									newInstructions.append(new PUSH(cpgen, result.longValue()));
								} else if (result instanceof Float) {
									newInstructions.append(new PUSH(cpgen, result.floatValue()));
								} else if (result instanceof Double) {
									newInstructions.append(new PUSH(cpgen, result.doubleValue()));
								} else {
									continue;
								}

								il.insert(ih, newInstructions);
								il.delete(ih, next2);

								System.out.println("REPLACED: " + value1 + " and " + value2
										+ " using " + instr3 + " with " + result);

								changed = true;
								break;
							} catch (TargetLostException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}

			mg.setMaxStack(); //change the method calc stack size
			mg.setMaxLocals(); //change the method calc # of vars 
			cgen.replaceMethod(method, mg.getMethod()); //update the methods in the class
		}

		this.optimized = cgen.getJavaClass(); //cgen is the edited class we are working on
	}

    //Check if this instruction is pushing a number. If yes, return the number. If not, return null.
	private Number getConstantValue(Instruction instruction, ConstantPoolGen cpgen) {
		if (instruction instanceof ConstantPushInstruction) {
			return ((ConstantPushInstruction) instruction).getValue();//Get the actual value from the ICONST ETC.
		}

		if (instruction instanceof LDC) {
			Object value = ((LDC) instruction).getValue(cpgen); //Get the actual value from the constant pool.
			if (value instanceof Number) {
				return (Number) value;
			}
		}

		if (instruction instanceof LDC2_W) {
			Object value = ((LDC2_W) instruction).getValue(cpgen); //2nd value
			if (value instanceof Number) {
				return (Number) value;
			}
		}

		return null;
	}
	
    //constant folding
	private Number foldOperation(Number value1, Number value2, Instruction instruction) {
		if (instruction instanceof IADD) {
			return value1.intValue() + value2.intValue();
		}
		if (instruction instanceof ISUB) {
			return value1.intValue() - value2.intValue();
		}
		if (instruction instanceof IMUL) {
			return value1.intValue() * value2.intValue();
		}
		if (instruction instanceof IDIV) {
			if (value2.intValue() == 0) {
				return null;
			}
			return value1.intValue() / value2.intValue();
		}

		if (instruction instanceof LADD) {
			return value1.longValue() + value2.longValue();
		}
		if (instruction instanceof LSUB) {
			return value1.longValue() - value2.longValue();
		}
		if (instruction instanceof LMUL) {
			return value1.longValue() * value2.longValue();
		}
		if (instruction instanceof LDIV) {
			if (value2.longValue() == 0L) {
				return null;
			}
			return value1.longValue() / value2.longValue();
		}

		if (instruction instanceof FADD) {
			return value1.floatValue() + value2.floatValue();
		}
		if (instruction instanceof FSUB) {
			return value1.floatValue() - value2.floatValue();
		}
		if (instruction instanceof FMUL) {
			return value1.floatValue() * value2.floatValue();
		}
		if (instruction instanceof FDIV) {
			return value1.floatValue() / value2.floatValue();
		}

		if (instruction instanceof DADD) {
			return value1.doubleValue() + value2.doubleValue();
		}
		if (instruction instanceof DSUB) {
			return value1.doubleValue() - value2.doubleValue();
		}
		if (instruction instanceof DMUL) {
			return value1.doubleValue() * value2.doubleValue();
		}
		if (instruction instanceof DDIV) {
			return value1.doubleValue() / value2.doubleValue();
		}

		return null;
	}

	
	public void write(String optimisedFilePath)
	{
		this.optimize();

		try {
			FileOutputStream out = new FileOutputStream(new File(optimisedFilePath));
			this.optimized.dump(out);
		} catch (FileNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
}