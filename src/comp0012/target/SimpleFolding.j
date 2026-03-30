; Jasmin Java assembler code that assembles the SimpleFolding example class

.source Type1.j
.class public comp0012/target/SimpleFolding
.super java/lang/Object

.method public <init>()V
	aload_0
	invokenonvirtual java/lang/Object/<init>()V
	return
.end method

.method public simple()V
	.limit stack 3

	getstatic java/lang/System/out Ljava/io/PrintStream;
	ldc 67
	ldc 12345
	iadd
	invokevirtual java/io/PrintStream/println(I)V
	return
.end method

.method public simpleSub()V
	.limit stack 3

	getstatic java/lang/System/out Ljava/io/PrintStream;
	ldc 100
	ldc 25
	isub
	invokevirtual java/io/PrintStream/println(I)V
	return
.end method

.method public simpleMul()V
	.limit stack 3

	getstatic java/lang/System/out Ljava/io/PrintStream;
	ldc 6
	ldc 7
	imul
	invokevirtual java/io/PrintStream/println(I)V
	return
.end method

.method public simpleDiv()V
	.limit stack 3

	getstatic java/lang/System/out Ljava/io/PrintStream;
	ldc 20
	ldc 5
	idiv
	invokevirtual java/io/PrintStream/println(I)V
	return
.end method

.method public simpleLongAdd()V
	.limit stack 5

	getstatic java/lang/System/out Ljava/io/PrintStream;
	ldc2_w 10000000000
	ldc2_w 2
	ladd
	invokevirtual java/io/PrintStream/println(J)V
	return
.end method

.method public simpleDoubleAdd()V
	.limit stack 5

	getstatic java/lang/System/out Ljava/io/PrintStream;
	ldc2_w 1.5
	ldc2_w 2.5
	dadd
	invokevirtual java/io/PrintStream/println(D)V
	return
.end method