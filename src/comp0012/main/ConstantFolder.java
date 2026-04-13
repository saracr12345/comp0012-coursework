package comp0012.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;

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
		cgen.setMajor(50);
		cgen.setMinor(0);
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

			// Main optimization loop
			// Task 1 and 2 are executed alternately within a single loop
			// Repeat each pass until no further changes occur
			boolean changed = true;

			while (changed) {
				changed = false;

				Map<Integer, Number> constantVariables = buildConstantVariableMap(il, cpgen);


				// Task 1: Simple Constant Folding
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

					if (value1 == null || value2 == null) continue;
                    if (!(instr3 instanceof ArithmeticInstruction)) continue;

					Number result = foldOperation(value1, value2, instr3);
					if (result == null) continue;

					// Create a new constant push instruction
					Instruction newInstr = buildPush(cpgen, result);
					if (newInstr == null) continue;

					// 1. Overwrite the original instruction at ih with setInstruction -> any branches targeting ih remain intact
					// 2. next1 and next2 instructions (operand push + operator) are no longer needed, 
					// so use redirectBranches to move each branch target to the instruction after next2, then delete them -> can be deleted without causing a TargetLostException
					ih.setInstruction(newInstr);

					try{
						
						il.delete(next1,next2);
					} catch (TargetLostException e) {
						// Since redirectBranches has already been done, should not reach this point.
						// If reach here, do not ignore it instead, redirect the targets to ih
						for (InstructionHandle lostTarget : e.getTargets()) {
                            InstructionTargeter[] targeters = lostTarget.getTargeters();
                            if (targeters != null) {
                                for (InstructionTargeter targeter : targeters) {
                                    targeter.updateTarget(lostTarget, ih);
                                }
                            }
                        }
                    }
					System.out.println("[Task1] FOLDED: " + value1 + " op " + value2 + " ("+ instr3 + ") ->" + result);
					changed = true;
					break;
				}
				if (changed) continue;
					// Task 2: Constant Variable Propagation
					// Replace the instruction that loads a constant variable with an actual constant push
					// Using setInstruction ensures the safety of jump targets (If the LoadInstruction is a branch target, it remains intact)
					for (InstructionHandle ih = il.getStart(); ih != null; ih = ih.getNext()){
						if (!(ih.getInstruction() instanceof LoadInstruction)) continue;

						int index = ((LoadInstruction) ih.getInstruction()).getIndex();
						if (!constantVariables.containsKey(index)) continue;

						Number constVal = constantVariables.get(index);
						Instruction newInstr = buildPush(cpgen, constVal);
						if (newInstr == null) continue;

						ih.setInstruction(newInstr);
						System.out.println("[Task2] PROPAGATED: iload/lload/fload/dload var# " + index + " ->" + constVal);
						changed = true;
						break;
					}
				
				if (changed) continue;

					// Task 3: Dynamic Variable Propagation
					if (propagateDynamicConstants(il, cpgen)) {
						changed = true;
						continue;
					}

					// Task 4: Constant conditional branch folding
					if (foldConstantBranches(il, cpgen)) {
						changed = true;
						continue;
					}
				
			}
			mg.removeLocalVariables();
            mg.removeLineNumbers();

			for (Attribute a : mg.getCodeAttributes()) {
                if (a.getClass().getSimpleName().contains("StackMap")) {
                    mg.removeCodeAttribute(a);
                }
            }
			il.setPositions(true);
			mg.setMaxStack(); //change the method calc stack size
			mg.setMaxLocals(); //change the method calc # of vars 
			cgen.replaceMethod(method, mg.getMethod()); //update the methods in the class
		}

		this.optimized = cgen.getJavaClass(); //cgen is the edited class we are working on
	}

	// Task 3: Dynamic variable propagation
	// Track the currently-known constant for each local variable slot inside straight-line code.
	// Clear the map at control-flow join points / block boundaries to stay conservative.
	private boolean propagateDynamicConstants(InstructionList il, ConstantPoolGen cpgen) {
		Map<Integer, Number> currentConstants = new HashMap<>();
		Set<InstructionHandle> branchTargets = collectBranchTargets(il);

		for (InstructionHandle ih = il.getStart(); ih != null; ih = ih.getNext()) {

			// Start of a new basic block: forget previous assumptions
			if (ih != il.getStart() && branchTargets.contains(ih)) {
				currentConstants.clear();
			}

			// If the previous instruction ended control flow, also clear
			InstructionHandle prevHandle = ih.getPrev();
			if (prevHandle != null) {
				Instruction prevInstr = prevHandle.getInstruction();
				if (prevInstr instanceof BranchInstruction || prevInstr instanceof ReturnInstruction) {
					currentConstants.clear();
				}
			}

			Instruction instr = ih.getInstruction();

			// If we load a variable whose current value is known, replace the load with a constant push
			if (instr instanceof LoadInstruction) {
				int idx = ((LoadInstruction) instr).getIndex();
				if (currentConstants.containsKey(idx)) {
					Number constVal = currentConstants.get(idx);
					Instruction newInstr = buildPush(cpgen, constVal);
					if (newInstr != null) {
						ih.setInstruction(newInstr);
						System.out.println("[Task3] DYNAMIC PROPAGATED: var# " + idx + " -> " + constVal);
						return true;
					}
				}
			}

			// Record constant assignments
			if (instr instanceof StoreInstruction) {
				int idx = ((StoreInstruction) instr).getIndex();
				InstructionHandle prev = ih.getPrev();
				Number constVal = null;

				if (prev != null) {
					constVal = getConstantValue(prev.getInstruction(), cpgen);
				}

				if (constVal != null) {
					currentConstants.put(idx, constVal);
				} else {
					currentConstants.remove(idx);
				}
				continue;
			}

			// Handle iinc specially: update tracked integer constant if known
			if (instr instanceof IINC) {
				int idx = ((IINC) instr).getIndex();
				if (currentConstants.containsKey(idx)) {
					Number oldValue = currentConstants.get(idx);
					currentConstants.put(idx, oldValue.intValue() + ((IINC) instr).getIncrement());
				} else {
					currentConstants.remove(idx);
				}
			}
		}

		return false;
	}

	private Set<InstructionHandle> collectBranchTargets(InstructionList il) {
		Set<InstructionHandle> targets = new HashSet<>();

		for (InstructionHandle ih = il.getStart(); ih != null; ih = ih.getNext()) {
			Instruction instr = ih.getInstruction();

			if (instr instanceof BranchInstruction) {
				BranchInstruction branch = (BranchInstruction) instr;
				targets.add(branch.getTarget());

				if (branch instanceof Select) {
					for (InstructionHandle target : ((Select) branch).getTargets()) {
						targets.add(target);
					}
				}
			}
		}

		return targets;
	}

	// Task 2 Preprocessing Method
	// 1. Scan all instructions and count the number of stores for each variable index.
	// 2. For variables stored exactly once, check if the instruction immediately preceding the store is a constant push. If so, register it in constantVariables.
	private Map<Integer, Number> buildConstantVariableMap(InstructionList il, ConstantPoolGen cpgen)
	{
		// 1. count the number of stores
		Map<Integer, Integer> storeCounts = new HashMap<>();
		for (InstructionHandle ih = il.getStart(); ih != null; ih = ih.getNext()){
			Instruction instr = ih.getInstruction();
			if(instr instanceof StoreInstruction) {
				int idx = ((StoreInstruction) instr).getIndex();
				storeCounts.put(idx, storeCounts.getOrDefault(idx,0) + 1);
			}else if (instr instanceof IINC) {
				int idx = ((IINC) instr).getIndex();
				storeCounts.put(idx, storeCounts.getOrDefault(idx, 0) + 1);
			}
		}

		// 2. Extract constantvariables stored exactly once
		Map<Integer, Number> constantVariables = new HashMap<>();
		for (InstructionHandle ih = il.getStart(); ih != null; ih = ih.getNext()){
			if (!(ih.getInstruction() instanceof StoreInstruction)) continue;

			int idx = ((StoreInstruction) ih.getInstruction()).getIndex();

			// Prevent Integer object comparison bug; use .intValue()
			if (storeCounts.get(idx).intValue() != 1) continue;

			InstructionHandle prev = ih.getPrev();
			if (prev == null) continue;

			Number constVal = getConstantValue(prev.getInstruction(), cpgen);
			if (constVal == null) continue;

			constantVariables.put(idx, constVal);
		}

		return constantVariables;
	}


	private boolean foldConstantBranches(InstructionList il, ConstantPoolGen cpgen) {
		for (InstructionHandle ih = il.getStart(); ih != null; ih = ih.getNext()) {
			Instruction instr1 = ih.getInstruction();

			// Case 1: unary branch
			// Pattern: CONST, IFxx
			InstructionHandle next1 = ih.getNext();
			if (next1 != null) {
				Instruction instr2 = next1.getInstruction();

				if (instr2 instanceof IFEQ
						|| instr2 instanceof IFNE
						|| instr2 instanceof IFLT
						|| instr2 instanceof IFLE
						|| instr2 instanceof IFGT
						|| instr2 instanceof IFGE) {

					Number value = getConstantValue(instr1, cpgen);
					if (value != null) {
						Boolean taken = evaluateUnaryBranch(value, (IfInstruction) instr2);
						if (taken != null) {
							// constant push is no longer needed
							ih.setInstruction(new NOP());

							InstructionHandle fallthrough = next1.getNext();
							if (fallthrough == null) continue;

							if (taken) {
								next1.setInstruction(new GOTO(((IfInstruction) instr2).getTarget()));
							} else {
								next1.setInstruction(new GOTO(fallthrough));
							}

							System.out.println("[Task4] FOLDED UNARY BRANCH: " + value + " (" + instr2 + ") -> "
									+ (taken ? "taken" : "not taken"));
							return true;
						}
					}
				}
			}

			// Case 2: binary integer compare
			// Pattern: CONST, CONST, IF_ICMPxx
			if (next1 == null) continue;
			InstructionHandle next2 = next1.getNext();
			if (next2 == null) continue;

			Instruction instr2 = next1.getInstruction();
			Instruction instr3 = next2.getInstruction();

			Number value1 = getConstantValue(instr1, cpgen);
			Number value2 = getConstantValue(instr2, cpgen);

			if (value1 == null || value2 == null) continue;

			Boolean taken = evaluateBinaryBranch(value1, value2, instr3);
			if (taken == null) continue;

			// constant pushes are no longer needed
			ih.setInstruction(new NOP());
			next1.setInstruction(new NOP());

			InstructionHandle fallthrough = next2.getNext();
			if (fallthrough == null) continue;

			if (taken) {
				next2.setInstruction(new GOTO(((IfInstruction) instr3).getTarget()));
			} else {
				next2.setInstruction(new GOTO(fallthrough));
			}

			System.out.println("[Task4] FOLDED BINARY BRANCH: " + value1 + " ? " + value2 + " (" + instr3 + ") -> "
					+ (taken ? "taken" : "not taken"));
			return true;
		}

		return false;
	}
	

	private Boolean evaluateUnaryBranch(Number value, IfInstruction branch) {
		int v = value.intValue();

		if (branch instanceof IFEQ) return v == 0;
		if (branch instanceof IFNE) return v != 0;
		if (branch instanceof IFLT) return v < 0;
		if (branch instanceof IFLE) return v <= 0;
		if (branch instanceof IFGT) return v > 0;
		if (branch instanceof IFGE) return v >= 0;

		return null;
	}

	private Boolean evaluateBinaryBranch(Number value1, Number value2, Instruction instruction) {
		if (!(instruction instanceof IfInstruction)) return null;

		int a = value1.intValue();
		int b = value2.intValue();

		if (instruction instanceof IF_ICMPEQ) return a == b;
		if (instruction instanceof IF_ICMPNE) return a != b;
		if (instruction instanceof IF_ICMPLT) return a < b;
		if (instruction instanceof IF_ICMPLE) return a <= b;
		if (instruction instanceof IF_ICMPGT) return a > b;
		if (instruction instanceof IF_ICMPGE) return a >= b;

		return null;
	}


	// Common helper - return the appropriate PUSH instruction for the Number type
	private Instruction buildPush(ConstantPoolGen cpgen, Number value) {
		InstructionList tmp = new InstructionList();
        if (value instanceof Integer) tmp.append(new org.apache.bcel.generic.PUSH(cpgen, value.intValue()));
        else if (value instanceof Long) tmp.append(new org.apache.bcel.generic.PUSH(cpgen, value.longValue()));
        else if (value instanceof Float) tmp.append(new org.apache.bcel.generic.PUSH(cpgen, value.floatValue()));
        else if (value instanceof Double) tmp.append(new org.apache.bcel.generic.PUSH(cpgen, value.doubleValue()));
        else return null;
        return tmp.getStart().getInstruction();
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